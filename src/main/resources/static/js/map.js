// US2 — Exploración por capas con Leaflet. Todas las operaciones requieren
// sesión; ante 401 el cliente api.js redirige a /login.html.
(function () {
  const map = L.map('map').setView([4.0, -73.5], 6);
  const tileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '&copy; OpenStreetMap'
  });

  // Las capas densas de puntos se dibujan en Canvas. Crear miles de nodos SVG
  // (6.826 movimientos en masa) puede hacer que el navegador omita o retrase
  // su pintura. Los panes garantizan además que los puntos queden sobre las
  // geometrías de área y las fallas.
  map.createPane('geoPolygons');
  map.getPane('geoPolygons').style.zIndex = 410;
  map.createPane('geoLines');
  map.getPane('geoLines').style.zIndex = 430;
  map.createPane('geoPoints');
  map.getPane('geoPoints').style.zIndex = 450;

  let offlineFillAdded = false;

  function addColombiaBasemap(basemap) {
    // Contorno sutil de Colombia siempre visible (orientación), sin relleno,
    // para no competir con las capas seleccionadas.
    L.geoJSON(basemap, {
      style: { color: '#475569', weight: 1.5, fill: false },
      interactive: false
    }).addTo(map);
    // Relleno oscuro SOLO como fallback si los tiles de OSM no cargan (offline).
    tileLayer.on('tileerror', () => {
      if (offlineFillAdded) return;
      offlineFillAdded = true;
      L.geoJSON(basemap, {
        style: { color: '#475569', weight: 1, fill: true, fillColor: '#1e293b', fillOpacity: 0.9 },
        interactive: false
      }).addTo(map);
    });
    tileLayer.addTo(map);
  }

  const STYLE = {
    MOVIMIENTO_EN_MASA: { color: '#e45756', radius: 3.5, fillOpacity: 0.82, geometry: 'point' },
    FALLA_GEOLOGICA: { color: '#8b5bd6', weight: 1.7, geometry: 'line' },
    UNIDAD_GEOLOGICA: { color: '#79c957', weight: 0.8, fillOpacity: 0.18, geometry: 'polygon' },
    DOMINIO_TECTONICO: { color: '#f5bf18', weight: 1.2, fillOpacity: 0.12, geometry: 'polygon' },
    VOLCAN: { color: '#f47a32', radius: 6, fillOpacity: 0.9, geometry: 'triangle' }
  };

  const state = {
    layers: [],
    active: new Set(),
    filters: {},   // domain -> [{attribute, value}]
    groups: {},    // domain -> L.LayerGroup
    loadVersion: {}, // domain -> versión de la última carga solicitada
    visibleFeatures: {},
    filterDomain: null,
    activeTool: 'explore'
  };

  const zoneCircles = [];
  let coordinatePicker = null;
  let geometryDrawer = null;
  let adminGeometryLayer = null;
  let adminEntitiesLayer = null;

  function renderAdminEntities(entities) {
    if (adminEntitiesLayer && map.hasLayer(adminEntitiesLayer)) map.removeLayer(adminEntitiesLayer);
    const features = (entities || []).map((entity) => ({
      type: 'Feature',
      id: entity.id,
      properties: { ...(entity.attributes || {}), origin: entity.origin, _domain: entity.domain },
      geometry: entity.geometry
    }));
    adminEntitiesLayer = L.geoJSON({ type: 'FeatureCollection', features }, {
      style: (feature) => {
        const color = domainStyle(feature.properties._domain).color;
        return { color, weight: 3, fillColor: color, fillOpacity: 0.2 };
      },
      pointToLayer: (feature, latlng) => L.circleMarker(latlng, {
        radius: 7, color: '#fff', weight: 2,
        fillColor: domainStyle(feature.properties._domain).color, fillOpacity: 1
      }),
      onEachFeature: (feature, layer) => layer.on('click', () => showEntity(feature))
    });
    if (state.activeTool === 'admin') adminEntitiesLayer.addTo(map);
  }

  function setAdminEntitiesVisible(visible) {
    if (!adminEntitiesLayer) return;
    if (visible && !map.hasLayer(adminEntitiesLayer)) adminEntitiesLayer.addTo(map);
    if (!visible && map.hasLayer(adminEntitiesLayer)) map.removeLayer(adminEntitiesLayer);
  }

  function clearAdminGeometry() {
    if (adminGeometryLayer) map.removeLayer(adminGeometryLayer);
    adminGeometryLayer = null;
  }

  function renderAdminGeometry(geometry) {
    clearAdminGeometry();
    if (!geometry) return;
    adminGeometryLayer = L.geoJSON({ type: 'Feature', properties: {}, geometry }, {
      style: { color: '#0f766e', weight: 3, fillColor: '#14b8a6', fillOpacity: 0.16 },
      pointToLayer: (_feature, latlng) => L.circleMarker(latlng, {
        radius: 7, color: '#fff', weight: 2, fillColor: '#0f766e', fillOpacity: 1
      })
    }).addTo(map);
  }

  function cancelGeometryDraw(clearGeometry = true) {
    if (geometryDrawer) {
      geometryDrawer.button.classList.remove('active');
      geometryDrawer.button.textContent = geometryDrawer.originalLabel;
    }
    geometryDrawer = null;
    map.getContainer().classList.remove('geometry-draw-active');
    if (clearGeometry) clearAdminGeometry();
    updateMapStatus();
  }

  function startGeometryDraw(kind, button, onComplete) {
    cancelCoordinatePick();
    cancelGeometryDraw(true);
    geometryDrawer = { kind, button, onComplete, points: [], originalLabel: button.textContent };
    button.classList.add('active');
    button.textContent = 'Cancelar dibujo';
    map.getContainer().classList.add('geometry-draw-active');
    document.getElementById('map-status').innerHTML = `<span class="pick-pulse"></span><span>${kind === 'Point' ? 'Haz clic para ubicar el punto' : 'Haz clic para agregar vértices'}</span>`;
  }

  function geometryFromDraft() {
    if (!geometryDrawer) return null;
    const coordinates = geometryDrawer.points.map((point) => [point.lng, point.lat]);
    if (geometryDrawer.kind === 'Point' && coordinates.length === 1) return { type: 'Point', coordinates: coordinates[0] };
    if (geometryDrawer.kind === 'LineString' && coordinates.length >= 2) return { type: 'LineString', coordinates };
    if (geometryDrawer.kind === 'Polygon' && coordinates.length >= 3) return { type: 'Polygon', coordinates: [[...coordinates, coordinates[0]]] };
    return null;
  }

  function finishGeometryDraw() {
    const geometry = geometryFromDraft();
    if (!geometry) return false;
    const onComplete = geometryDrawer.onComplete;
    cancelGeometryDraw(false);
    renderAdminGeometry(geometry);
    onComplete(geometry);
    return true;
  }

  function addGeometryVertex(latlng) {
    geometryDrawer.points.push(latlng);
    const geometry = geometryFromDraft();
    if (geometryDrawer.kind === 'Point') {
      finishGeometryDraw();
      return;
    }
    clearAdminGeometry();
    const latLngs = geometryDrawer.points.map((point) => [point.lat, point.lng]);
    adminGeometryLayer = geometryDrawer.kind === 'Polygon'
      ? L.polygon(latLngs, { color: '#0f766e', weight: 3, fillOpacity: 0.14 }).addTo(map)
      : L.polyline(latLngs, { color: '#0f766e', weight: 3 }).addTo(map);
  }

  function cancelCoordinatePick() {
    if (!coordinatePicker) return;
    coordinatePicker.button.classList.remove('active');
    coordinatePicker.button.innerHTML = coordinatePicker.originalContent;
    map.getContainer().classList.remove('coordinate-pick-active');
    coordinatePicker = null;
    updateMapStatus();
  }

  function startCoordinatePick(button, onSelect) {
    if (coordinatePicker?.button === button) {
      cancelCoordinatePick();
      return;
    }
    cancelCoordinatePick();
    coordinatePicker = { button, onSelect, originalContent: button.innerHTML };
    button.classList.add('active');
    button.textContent = 'Cancelar selección';
    map.getContainer().classList.add('coordinate-pick-active');
    document.getElementById('map-status').innerHTML = '<span class="pick-pulse"></span><span>Haz clic en el mapa para elegir la coordenada</span>';
  }

  function drawZoneCircle(lon, lat, radiusMeters) {
    const circle = L.circle([lat, lon], {
      radius: radiusMeters,
      color: '#0284c7',
      weight: 2,
      fill: true,
      fillColor: '#38bdf8',
      fillOpacity: 0.08,
      dashArray: '7 5'
    }).addTo(map);
    const marker = L.marker([lat, lon], {
      icon: L.divIcon({
        className: 'analysis-center-wrapper',
        html: '<span class="analysis-center" aria-hidden="true"></span>',
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      })
    }).addTo(map);
    zoneCircles.push(circle, marker);
    map.fitBounds(circle.getBounds(), { padding: [45, 45], maxZoom: 14 });
    return circle;
  }

  function clearZoneCircles() {
    zoneCircles.forEach((circle) => map.removeLayer(circle));
    zoneCircles.length = 0;
  }

  function drawComparisonZones(zoneA, zoneB) {
    clearZoneCircles();
    const zones = [
      { ...zoneA, label: 'A', color: '#2563eb' },
      { ...zoneB, label: 'B', color: '#9333ea' }
    ];
    const bounds = L.latLngBounds();
    zones.forEach((zone) => {
      const circle = L.circle([zone.lat, zone.lon], {
        radius: zone.radiusMeters,
        color: zone.color,
        weight: 2,
        fill: true,
        fillColor: zone.color,
        fillOpacity: 0.08,
        dashArray: '7 5'
      }).addTo(map);
      const marker = L.marker([zone.lat, zone.lon], {
        icon: L.divIcon({
          className: 'comparison-center-wrapper',
          html: `<span class="comparison-center comparison-center-${zone.label.toLowerCase()}">${zone.label}</span>`,
          iconSize: [30, 30],
          iconAnchor: [15, 15]
        })
      }).addTo(map);
      zoneCircles.push(circle, marker);
      bounds.extend(circle.getBounds());
    });
    if (bounds.isValid()) map.fitBounds(bounds, { padding: [45, 45], maxZoom: 14 });
  }

  window.GeoInsightMap = {
    drawZoneCircle,
    drawComparisonZones,
    clearZoneCircles,
    startCoordinatePick,
    cancelCoordinatePick,
    startGeometryDraw,
    finishGeometryDraw,
    cancelGeometryDraw,
    renderAdminGeometry,
    clearAdminGeometry,
    renderAdminEntities,
    domainColor: (domain) => domainStyle(domain).color,
    closeEntityInfo
  };

  function domainStyle(domain) {
    return STYLE[domain] || { color: '#94a3b8' };
  }

  const CanvasPointLayer = L.Layer.extend({
    initialize(domain, features, style) {
      this.domain = domain;
      this.features = features;
      this.style = style;
      this.screenPoints = [];
    },

    onAdd(leafletMap) {
      this.leafletMap = leafletMap;
      this.canvas = L.DomUtil.create('canvas', 'geo-points-canvas');
      this.canvas.style.pointerEvents = 'none';
      leafletMap.getPane('geoPoints').appendChild(this.canvas);
      leafletMap.on('moveend zoomend resize', this.redraw, this);
      this.redraw();
    },

    onRemove(leafletMap) {
      leafletMap.off('moveend zoomend resize', this.redraw, this);
      this.canvas?.remove();
      this.canvas = null;
      this.screenPoints = [];
    },

    redraw() {
      if (!this.canvas || !this.leafletMap) return;
      const size = this.leafletMap.getSize();
      const ratio = window.devicePixelRatio || 1;
      const topLeft = this.leafletMap.containerPointToLayerPoint([0, 0]);
      L.DomUtil.setPosition(this.canvas, topLeft);
      this.canvas.style.width = `${size.x}px`;
      this.canvas.style.height = `${size.y}px`;
      this.canvas.width = Math.round(size.x * ratio);
      this.canvas.height = Math.round(size.y * ratio);

      const context = this.canvas.getContext('2d');
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
      context.clearRect(0, 0, size.x, size.y);
      this.screenPoints = [];

      this.features.forEach((feature) => {
        if (!feature.geometry || feature.geometry.type !== 'Point') return;
        const [lon, lat] = feature.geometry.coordinates;
        const point = this.leafletMap.latLngToContainerPoint([lat, lon]);
        if (point.x < -12 || point.y < -12 || point.x > size.x + 12 || point.y > size.y + 12) return;
        this.screenPoints.push({ point, feature });
        this.drawPoint(context, point);
      });
    },

    drawPoint(context, point) {
      context.save();
      context.fillStyle = this.style.color;
      context.strokeStyle = '#ffffff';
      context.lineWidth = 1.25;
      context.beginPath();
      if (this.domain === 'VOLCAN') {
        context.moveTo(point.x, point.y - 7);
        context.lineTo(point.x + 7, point.y + 6);
        context.lineTo(point.x - 7, point.y + 6);
        context.closePath();
      } else {
        context.arc(point.x, point.y, 4, 0, Math.PI * 2);
      }
      context.fill();
      context.stroke();
      context.restore();
    },

    featureAt(containerPoint) {
      const hitRadius = this.domain === 'VOLCAN' ? 10 : 7;
      const hitRadiusSquared = hitRadius * hitRadius;
      for (let index = this.screenPoints.length - 1; index >= 0; index -= 1) {
        const candidate = this.screenPoints[index];
        const dx = candidate.point.x - containerPoint.x;
        const dy = candidate.point.y - containerPoint.y;
        if ((dx * dx) + (dy * dy) <= hitRadiusSquared) return candidate.feature;
      }
      return null;
    }
  });

  function createLayer(domain, features) {
    const style = domainStyle(domain);
    if (style.geometry === 'point' || style.geometry === 'triangle') {
      return new CanvasPointLayer(domain, features, style);
    }
    const pane = style.geometry === 'polygon'
      ? 'geoPolygons'
      : style.geometry === 'line' ? 'geoLines' : 'geoPoints';
    const layer = L.geoJSON(features, {
      pane,
      style: {
        color: style.color,
        weight: style.weight,
        fillOpacity: style.fillOpacity
      }
    });
    layer.on('click', (event) => showEntity(event.propagatedFrom.feature));
    return layer;
  }

  const PROVIDER_ATTRIBUTES = new Set([
    'OBJECTID', 'GlobalID', 'FID', 'ESRI_OID', 'ID',
    'Shape__Length', 'Shape__Area', 'SHAPE_Leng', 'SHAPE_Area'
  ]);

  const ATTRIBUTE_LABELS = {
    Shape__Length: 'Longitud',
    SHAPE_Leng: 'Longitud'
  };

  function displayAttributes(attributes) {
    return Object.entries(attributes)
      .filter(([key, value]) => value !== null && value !== undefined && value !== ''
        && !PROVIDER_ATTRIBUTES.has(key))
      .map(([key, value]) => [ATTRIBUTE_LABELS[key] || key, value]);
  }

  function showEntity(feature) {
    const container = document.getElementById('entity-info');
    if (!feature || !feature.properties) {
      closeEntityInfo();
      return;
    }
    const origin = feature.properties.origin || feature.origin;
    const originLabel = origin === 'GEOINSIGHT' ? 'GEOINSIGHT' : 'SGC';
    const entityTitle = ui.entityName({ id: feature.id, attributes: feature.properties });
    let html = '<div class="entity-head"><div>';
    html += `<span class="badge ${originLabel}">${originLabel}</span><h3 class="entity-title">${ui.escapeHtml(entityTitle)}</h3></div>`;
    html += '<button class="entity-close" type="button" aria-label="Cerrar">&times;</button></div>';
    html += '<div class="entity-content"><table class="attr-table">';
    const { origin: _origin, _domain: _internalDomain, ...attributes } = feature.properties;
    displayAttributes(attributes).forEach(([label, value]) => {
      html += `<tr><td>${ui.escapeHtml(label)}</td><td>${ui.escapeHtml(String(value))}</td></tr>`;
    });
    html += '</table></div>';
    container.innerHTML = html;
    container.classList.remove('hidden');
    container.querySelector('.entity-close').addEventListener('click', closeEntityInfo);
  }

  function closeEntityInfo() {
    const container = document.getElementById('entity-info');
    container.classList.add('hidden');
  }

  async function loadEntities(domain, opts = {}) {
    const requestVersion = (state.loadVersion[domain] || 0) + 1;
    state.loadVersion[domain] = requestVersion;
    const filters = (state.filters[domain] || []).map((f) => `${encodeURIComponent(f.attribute)}=${encodeURIComponent(f.value)}`).join('&');
    const url = filters ? `/api/entities/${domain}?${filters}` : `/api/entities/${domain}`;
    const data = await api.get(url);
    if (state.loadVersion[domain] !== requestVersion || !state.active.has(domain)) return 0;
    const group = state.groups[domain];
    group.clearLayers();
    const features = data.features || [];
    state.visibleFeatures[domain] = features;
    const layer = createLayer(domain, features);
    group.addLayer(layer);
    if (opts.fit) fitToResults(layer, features);
    return features.length;
  }

  function fitToResults(layer, features) {
    if (features.length === 0) return;
    const pointFeatures = features.filter((feature) => feature.geometry?.type === 'Point');
    if (pointFeatures.length === features.length) {
      if (pointFeatures.length === 1) {
        const [lon, lat] = pointFeatures[0].geometry.coordinates;
        map.setView([lat, lon], 12);
        return;
      }
      const pointBounds = L.latLngBounds(pointFeatures.map((feature) => {
        const [lon, lat] = feature.geometry.coordinates;
        return [lat, lon];
      }));
      if (pointBounds.isValid()) map.fitBounds(pointBounds, { padding: [40, 40], maxZoom: 15 });
      return;
    }
    const bounds = layer.getBounds();
    if (bounds.isValid()) map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
  }

  async function refreshActiveLayers() {
    await Promise.all([...state.active].map(loadEntities));
  }

  function buildLayerPanel() {
    const list = document.getElementById('layer-list');
    list.innerHTML = '';
    state.layers.forEach((layer) => {
      const row = document.createElement('div');
      row.className = 'layer-row';
      const style = domainStyle(layer.domain);
      row.dataset.geometry = style.geometry || 'point';
      row.style.setProperty('--layer-color', style.color);
      row.innerHTML = `
        <input type="checkbox" id="layer-${layer.domain}" data-domain="${layer.domain}" ${layer.dataAvailable === false ? 'disabled' : ''}>
        <span class="layer-swatch" aria-hidden="true"></span>
        <label for="layer-${layer.domain}">${ui.escapeHtml(layer.name)}${layer.dataAvailable === false ? '<span class="layer-missing"> DATOS NO DISPONIBLES</span>' : ''}</label>
        <span class="layer-count">${Number(layer.count).toLocaleString('es-CO')}</span>`;
      const checkbox = row.querySelector('input');
      checkbox.addEventListener('change', async () => {
        if (checkbox.checked) {
          state.active.add(layer.domain);
          await loadEntities(layer.domain);
        } else {
          state.active.delete(layer.domain);
          state.loadVersion[layer.domain] = (state.loadVersion[layer.domain] || 0) + 1;
          state.groups[layer.domain].clearLayers();
          state.filters[layer.domain] = [];
          state.visibleFeatures[layer.domain] = [];
        }
        rebuildFilterBuilder(layer.domain);
        updateMapStatus();
      });
      list.appendChild(row);
    });
  }

  function updateMapStatus() {
    const activeCount = state.active.size;
    document.getElementById('map-status').innerHTML = `<span class="pulse"></span><span>${activeCount} de ${state.layers.length} capas activas</span>`;
  }

  function getActiveFilterDomain() {
    const select = document.getElementById('filter-domain');
    return select && select.value ? select.value : [...state.active][0] || null;
  }

  function rebuildFilterBuilder(domain) {
    const builder = document.getElementById('filter-builder');
    const note = document.getElementById('filter-note');
    const domainSelect = document.getElementById('filter-domain');
    const attributeSelect = document.getElementById('filter-attribute');
    const activeDomains = [...state.active];
    if (activeDomains.length === 0) {
      domainSelect.innerHTML = '<option value="">Sin capas activas</option>';
      builder.classList.add('hidden');
      note.classList.remove('hidden');
      renderFilterList(null);
      renderFilterFeedback(null);
      renderFilterResults(null);
      return;
    }
    note.classList.add('hidden');
    builder.classList.remove('hidden');
    domainSelect.innerHTML = '';
    activeDomains.forEach((active) => {
      const layer = state.layers.find((l) => l.domain === active);
      const option = document.createElement('option');
      option.value = active;
      option.textContent = layer ? layer.name : active;
      domainSelect.appendChild(option);
    });
    if (!domain || !activeDomains.includes(domain)) domain = activeDomains[0];
    domainSelect.value = domain;
    state.filterDomain = domain;
    const metadata = state.layers.find((l) => l.domain === domain);
    attributeSelect.innerHTML = '';
    metadata.filterableAttributes.forEach((attr) => {
      const option = document.createElement('option');
      option.value = attr.name;
      option.textContent = `${attr.name} (${attr.values.length} valores)`;
      attributeSelect.appendChild(option);
    });
    attributeSelect.dispatchEvent(new Event('change'));
    renderFilterList(domain);
    renderFilterFeedback(null);
  }

  function renderFilterFeedback(domain, count) {
    const feedback = document.getElementById('filter-feedback');
    if (!domain) {
      feedback.innerHTML = '';
      return;
    }
    if (count === 0) {
      feedback.innerHTML = '<span class="empty">Sin coincidencias. El mapa no cambió.</span>';
    } else {
      feedback.innerHTML = `<span class="filter-ok">${count} resultado(s) en el mapa.</span>`;
    }
  }

  function bindFilterBuilder() {
    const attributeSelect = document.getElementById('filter-attribute');
    const valueSelect = document.getElementById('filter-value');
    document.getElementById('filter-domain').addEventListener('change', async () => {
      const selectedDomain = document.getElementById('filter-domain').value;
      const previousDomain = state.filterDomain;
      if (previousDomain && previousDomain !== selectedDomain) {
        state.filters[previousDomain] = [];
        if (state.active.has(previousDomain)) await loadEntities(previousDomain);
      }
      state.filterDomain = selectedDomain;
      rebuildFilterBuilder(selectedDomain);
      renderFilterResults(null);
    });
    attributeSelect.addEventListener('change', () => {
      const activeDomain = getActiveFilterDomain();
      const metadata = state.layers.find((l) => l.domain === activeDomain);
      const attr = metadata.filterableAttributes.find((a) => a.name === attributeSelect.value);
      valueSelect.innerHTML = '';
      attr.values.forEach((value) => {
        const option = document.createElement('option');
        option.value = value;
        option.textContent = value;
        valueSelect.appendChild(option);
      });
    });
    document.getElementById('filter-add').addEventListener('click', async () => {
      const activeDomain = getActiveFilterDomain();
      if (!activeDomain) return;
      const filters = state.filters[activeDomain] || (state.filters[activeDomain] = []);
      if (filters.some((filter) => filter.attribute === attributeSelect.value && filter.value === valueSelect.value)) return;
      filters.push({ attribute: attributeSelect.value, value: valueSelect.value });
      renderFilterList(activeDomain);
      const addButton = document.getElementById('filter-add');
      addButton.disabled = true;
      addButton.textContent = 'Actualizando resultados…';
      document.getElementById('filter-results').innerHTML = '<div class="filter-results-loading">Consultando registros…</div>';
      try {
        const count = await loadEntities(activeDomain, { fit: true });
        renderFilterFeedback(activeDomain, count);
        renderFilterResults(activeDomain);
      } catch (error) {
        ui.showError(document.getElementById('filter-feedback'), error.message);
      } finally {
        addButton.disabled = false;
        addButton.textContent = 'Aplicar filtro';
      }
    });
  }

  function renderFilterList(domain) {
    const list = document.getElementById('filter-list');
    list.innerHTML = '';
    if (!domain || !state.filters[domain]) return;
    const groups = new Map();
    state.filters[domain].forEach((filter, index) => {
      if (!groups.has(filter.attribute)) groups.set(filter.attribute, []);
      groups.get(filter.attribute).push({ ...filter, index });
    });
    [...groups.entries()].forEach(([attribute, filters], groupIndex) => {
      if (groupIndex > 0) list.insertAdjacentHTML('beforeend', '<div class="filter-operator and">Y</div>');
      const group = document.createElement('div');
      group.className = 'filter-group';
      group.innerHTML = `<strong>${ui.escapeHtml(attribute)}</strong>`;
      filters.forEach((filter, valueIndex) => {
        if (valueIndex > 0) group.insertAdjacentHTML('beforeend', '<span class="filter-operator or">O</span>');
        const chip = document.createElement('div');
        chip.className = 'filter-chip';
        chip.innerHTML = `<span>${ui.escapeHtml(filter.value)}</span><button class="danger" type="button" aria-label="Quitar filtro">&times;</button>`;
        chip.querySelector('button').addEventListener('click', async () => {
          state.filters[domain].splice(filter.index, 1);
          renderFilterList(domain);
          const count = await loadEntities(domain, { fit: true });
          renderFilterFeedback(domain, count);
          renderFilterResults(state.filters[domain].length > 0 ? domain : null);
        });
        group.appendChild(chip);
      });
      list.appendChild(group);
    });
  }

  function renderFilterResults(domain) {
    const container = document.getElementById('filter-results');
    container.innerHTML = '<div class="filter-results-heading"><strong>Resultados</strong></div>';
    if (!domain || !state.filters[domain]?.length) {
      container.insertAdjacentHTML('beforeend', '<div class="filter-results-empty">Aplica un filtro para consultar los registros coincidentes.</div>');
      return;
    }
    const features = state.visibleFeatures[domain] || [];
    if (features.length === 0) {
      container.insertAdjacentHTML('beforeend', '<div class="filter-results-empty">No se encontraron registros para estos criterios.</div>');
      container.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      return;
    }
    const limit = 250;
    const table = document.createElement('table');
    table.className = 'filter-results-table';
    table.innerHTML = '<thead><tr><th>Entidad</th><th>Origen</th></tr></thead><tbody></tbody>';
    const body = table.querySelector('tbody');
    features.slice(0, limit).forEach((feature) => {
      const row = document.createElement('tr');
      const name = ui.entityName({ id: feature.id, attributes: feature.properties || {} });
      row.innerHTML = `<td><button type="button">${ui.escapeHtml(name)}</button></td><td>${ui.escapeHtml(feature.properties?.origin || feature.origin || '—')}</td>`;
      row.querySelector('button').addEventListener('click', () => focusFeature(feature));
      body.appendChild(row);
    });
    container.innerHTML = `<div class="filter-results-heading"><strong>Registros encontrados</strong><span>${features.length}</span></div>`;
    container.appendChild(table);
    if (features.length > limit) {
      container.insertAdjacentHTML('beforeend', `<p class="filter-results-limit">Se muestran los primeros ${limit} registros.</p>`);
    }
    container.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function focusFeature(feature) {
    if (!feature?.geometry) return;
    if (feature.geometry.type === 'Point') {
      const [lon, lat] = feature.geometry.coordinates;
      map.setView([lat, lon], 14);
    } else {
      const bounds = L.geoJSON(feature).getBounds();
      if (bounds.isValid()) map.fitBounds(bounds, { padding: [45, 45], maxZoom: 14 });
    }
    showEntity(feature);
  }

  const TOOLS = ['explore', 'filters', 'context', 'zone', 'compare', 'admin', 'help'];

  function activateTool(tool) {
    cancelCoordinatePick();
    if (state.activeTool !== tool) cancelGeometryDraw(tool !== 'admin');
    state.activeTool = tool;
    document.getElementById('tool-drawer').classList.toggle('compare-mode', tool === 'compare');
    document.getElementById('tool-drawer').classList.toggle('admin-mode', tool === 'admin');
    setAdminEntitiesVisible(tool === 'admin');
    TOOLS.forEach((name) => {
      document.getElementById(`panel-${name}`).classList.toggle('hidden', name !== tool);
      document.getElementById(`nav-${name}`).classList.toggle('active', name === tool);
    });
    document.getElementById('tool-drawer').classList.remove('collapsed');
    setTimeout(() => map.invalidateSize(), 260);
  }

  function bindToolNavigation() {
    TOOLS.forEach((name) => {
      document.getElementById(`nav-${name}`).addEventListener('click', () => activateTool(name));
    });
    document.getElementById('sidebar-toggle').addEventListener('click', () => {
      document.getElementById('sidebar').classList.toggle('collapsed');
      setTimeout(() => map.invalidateSize(), 260);
    });
    document.getElementById('drawer-toggle').addEventListener('click', (event) => {
      const collapsed = document.getElementById('tool-drawer').classList.toggle('collapsed');
      event.currentTarget.textContent = collapsed ? '›' : '‹';
      setTimeout(() => map.invalidateSize(), 260);
    });
  }

  function bindCursorCoordinates() {
    const coordBar = document.getElementById('cursor-coords');
    map.on('mousemove', (event) => {
      coordBar.innerHTML = `<span>Coordenadas del cursor:</span><strong>${event.latlng.lat.toFixed(5)}°, ${event.latlng.lng.toFixed(5)}°</strong><i></i><span>EPSG:4326 (WGS 84)</span>`;
    });
  }

  function pointFeatureAt(containerPoint) {
    for (const domain of state.active) {
      const group = state.groups[domain];
      let found = null;
      group.eachLayer((layer) => {
        if (!found && typeof layer.featureAt === 'function') found = layer.featureAt(containerPoint);
      });
      if (found) return found;
    }
    return null;
  }

  async function init() {
    const me = await api.get('/api/auth/me');
    document.getElementById('account-name').textContent = me.username;
    document.getElementById('account-role').textContent = me.role === 'ADMIN' ? 'Administrador' : 'Usuario de consulta';
    document.getElementById('user-avatar').textContent = me.username.charAt(0).toUpperCase();
    window.GeoInsight = window.GeoInsight || {};
    window.GeoInsight.me = me;
    document.getElementById('help-admin').classList.toggle('hidden', !me.admin);
    window.GeoInsight.activeDomain = () => [...state.active][0] || null;
    window.GeoInsight.layers = () => state.layers;

    map.on('click', (event) => {
      if (geometryDrawer) {
        addGeometryVertex(event.latlng);
        return;
      }
      if (coordinatePicker) {
        const picker = coordinatePicker;
        picker.onSelect(event.latlng.lng, event.latlng.lat);
        cancelCoordinatePick();
        return;
      }
      const pointFeature = pointFeatureAt(event.containerPoint);
      if (pointFeature) {
        showEntity(pointFeature);
        return;
      }
      if (state.activeTool === 'context' && window.GeoInsight.onMapClick) {
        window.GeoInsight.onMapClick(event.latlng.lng, event.latlng.lat);
      }
    });

    const [basemap, layers] = await Promise.all([
      api.get('/api/basemap/colombia'),
      api.get('/api/layers')
    ]);

    addColombiaBasemap(basemap);

    state.layers = layers;
    layers.forEach((layer) => {
      state.groups[layer.domain] = L.layerGroup().addTo(map);
      state.filters[layer.domain] = [];
      state.loadVersion[layer.domain] = 0;
      state.visibleFeatures[layer.domain] = [];
    });
    buildLayerPanel();
    bindFilterBuilder();
    bindToolNavigation();
    bindCursorCoordinates();
    rebuildFilterBuilder(null);
    updateMapStatus();

    document.getElementById('logout-button').addEventListener('click', async () => {
      await api.post('/api/auth/logout');
      window.location.href = '/login.html';
    });
  }

  init().catch((error) => {
    document.getElementById('layer-list').textContent = error.message;
  });
})();
