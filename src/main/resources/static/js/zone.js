// US4 — Análisis descriptivo de una zona (FR-010, FR-012). Solo indicadores,
// sin conclusiones de riesgo. Las tarjetas actúan como filtros: al seleccionar
// un dominio, su resumen aparece en el panel y sus entidades se resaltan en el
// mapa; sin filtros se muestran todos los dominios colapsables.
(function () {
  const result = ui.$('zone-result');
  let lastData = null;
  let filters = {};

  const DOMAINS = [
    { key: 'massMovements', label: 'Movimientos en masa', domain: 'MOVIMIENTO_EN_MASA', distributionLabel: 'Distribución por tipo' },
    { key: 'faults', label: 'Fallas', domain: 'FALLA_GEOLOGICA', distributionLabel: 'Distribución por tipo' },
    { key: 'geologicalUnits', label: 'Unidades geológicas', domain: 'UNIDAD_GEOLOGICA', distributionLabel: 'Distribución por edad' },
    { key: 'tectonicDomains', label: 'Dominios tectónicos', domain: 'DOMINIO_TECTONICO', distributionLabel: 'Distribución por dominio' },
    { key: 'volcanoes', label: 'Volcanes', domain: 'VOLCAN', distributionLabel: null }
  ];

  async function runZone() {
    const point = ui.numberInput('zone');
    const radius = parseFloat(ui.$('zone-radius').value);
    if (!ui.validCoordinate(point) || !Number.isFinite(radius) || radius <= 0) {
      ui.showError(result, 'Ingrese coordenadas y un radio válido en metros.');
      return;
    }
    try {
      result.innerHTML = '<div class="skeleton">Calculando indicadores…</div>';
      const data = await api.post('/api/zones/analyze', { lon: point.lon, lat: point.lat, radiusMeters: radius });
      lastData = data;
      filters = {};
      GeoInsightMap.clearZoneAnalysis();
      GeoInsightMap.drawZoneCircle(point.lon, point.lat, radius);
      renderAll();
    } catch (error) {
      ui.showError(result, error.message);
    }
  }

  function renderAll() {
    if (!lastData) return;
    renderResult(lastData);
    syncMap(lastData);
    updatePill();
  }

  function renderResult(data) {
    result.innerHTML = `
      <div class="zone-analysis">
        <div id="zone-active-filter" class="zone-active-filter hidden"></div>
        ${summaryCards(data)}
        ${blocks(data)}
      </div>`;
    bindInteractions(data);
  }

  function summaryCards(data) {
    return `<div class="zone-summary">${DOMAINS.map(({ key, label }) => {
      const breakdown = data[key];
      const value = breakdown.dataAvailable === false ? '—' : ui.formatNumber(breakdown.count);
      const disabled = breakdown.dataAvailable === false || breakdown.count === 0;
      const active = Object.prototype.hasOwnProperty.call(filters, key) ? ' active' : '';
      return `<button type="button" class="zone-card${active}" data-zone-domain="${key}" ${disabled ? 'disabled' : ''}>
        <span class="zone-card-value">${ui.escapeHtml(value)}</span>
        <span class="zone-card-label">${ui.escapeHtml(label)}</span>
      </button>`;
    }).join('')}</div>`;
  }

  function blocks(data) {
    const active = Object.keys(filters).length > 0;
    const keys = active ? Object.keys(filters) : DOMAINS.map((item) => item.key);
    return keys.map((key) => {
      const meta = DOMAINS.find((item) => item.key === key);
      const breakdown = data[key];
      const open = active || key !== 'geologicalUnits';
      return `<details class="zone-block" ${open ? 'open' : ''}>
        <summary class="zone-block-heading">
          <span class="zone-block-title">${ui.escapeHtml(meta.label)}</span>
          <span class="zone-total">${totalText(breakdown)}</span>
        </summary>
        <div class="zone-block-body">${blockBody(key, breakdown)}</div>
      </details>`;
    }).join('');
  }

  function blockBody(key, breakdown) {
    if (breakdown.dataAvailable === false) {
      return `<p class="zone-empty">Información no disponible para ${phrase(key)} en la zona seleccionada.</p>`;
    }
    if (breakdown.count === 0) {
      return `<p class="zone-empty">No se encontraron ${phrase(key)} en la zona seleccionada.</p>`;
    }
    if (key === 'massMovements') {
      const byTipo = sortedEntries(breakdown.byTipo);
      return `${dominantBlock(byTipo[0], breakdown.count)}
        ${distributionBars('Distribución por tipo', byTipo, breakdown.count, true)}
        ${collapsible('Ver desglose por subtipo', breakdown.bySubtipo)}
        ${collapsible('Ver clasificación cartográfica', breakdown.byClasMapa)}`;
    }
    if (key === 'volcanoes') {
      return collapsibleNames(`Ver volcanes de la zona (${breakdown.count})`, breakdown.entities);
    }
    return distributionBars(DOMAINS.find((item) => item.key === key).distributionLabel,
      sortedEntries(breakdown.byTipo), breakdown.count, false);
  }

  function phrase(key) {
    return {
      massMovements: 'movimientos en masa',
      faults: 'fallas',
      geologicalUnits: 'unidades geológicas',
      tectonicDomains: 'dominios tectónicos',
      volcanoes: 'volcanes'
    }[key];
  }

  function totalText(breakdown) {
    return breakdown.dataAvailable === false ? '—' : `Total: ${ui.formatNumber(breakdown.count)}`;
  }

  function dominantBlock(dominant, total) {
    if (!dominant) return '';
    const [name, count] = dominant;
    return `<div class="zone-dominant">
      <span class="zone-dominant-label">Tipo predominante</span>
      <strong class="zone-dominant-name">${ui.escapeHtml(name)}</strong>
      <span class="zone-dominant-meta">${ui.formatNumber(count)} registros · ${ui.formatPercent(count, total)}</span>
    </div>`;
  }

  function distributionBars(title, entries, total, interactive) {
    if (entries.length === 0) return '';
    const rows = entries.map(([name, count]) => {
      const width = (count / total) * 100;
      const inner = `
        <span class="zone-bar-label">${ui.escapeHtml(name)}</span>
        <span class="zone-bar-track" aria-hidden="true"><span class="zone-bar-fill" style="width:${width.toFixed(1)}%"></span></span>
        <strong class="zone-bar-count">${ui.formatNumber(count)}</strong>
        <span class="zone-bar-percent">${ui.formatPercent(count, total)}</span>`;
      return interactive
        ? `<button type="button" class="zone-bar-row" data-zone-bar="${ui.escapeHtml(name)}">${inner}</button>`
        : `<div class="zone-bar-row zone-bar-row-static">${inner}</div>`;
    }).join('');
    return `<div class="zone-distribution"><h4>${ui.escapeHtml(title)}</h4>${rows}</div>`;
  }

  function collapsible(title, distribution) {
    const entries = sortedEntries(distribution);
    if (entries.length === 0) return '';
    const rows = entries.map(([name, count]) =>
      `<li class="zone-detail-row"><span>${ui.escapeHtml(name)}</span><strong>${ui.formatNumber(count)}</strong></li>`).join('');
    return `<details class="zone-collapsible">
      <summary>${ui.escapeHtml(title)} (${entries.length})</summary>
      <ul class="zone-detail-list">${rows}</ul>
    </details>`;
  }

  function collapsibleNames(title, entities) {
    if (!entities || entities.length === 0) return '';
    const rows = entities.map((entity) =>
      `<li class="zone-detail-row"><span>${ui.escapeHtml(ui.entityName(entity))}</span></li>`).join('');
    return `<details class="zone-collapsible">
      <summary>${ui.escapeHtml(title)}</summary>
      <ul class="zone-detail-list">${rows}</ul>
    </details>`;
  }

  function bindInteractions(data) {
    result.querySelectorAll('[data-zone-domain]').forEach((element) => {
      element.addEventListener('click', () => toggleDomain(data, element.dataset.zoneDomain));
    });
    result.querySelectorAll('[data-zone-bar]').forEach((element) => {
      element.addEventListener('click', () => selectTipo(data, element.dataset.zoneBar));
    });
    const clear = result.querySelector('.zone-active-clear');
    if (clear) clear.addEventListener('click', clearFilters);
  }

  function toggleDomain(data, key) {
    if (filters[key]) {
      delete filters[key];
    } else {
      filters[key] = allEntities(data, key).map(entityFeature);
    }
    renderAll();
  }

  function selectTipo(data, tipo) {
    const breakdown = data.massMovements;
    const isUnclassified = tipo === 'Sin clasificar';
    filters.massMovements = (breakdown.entities || [])
      .filter((entity) => {
        const value = entity.attributes && entity.attributes.TIPO;
        if (isUnclassified) return value === undefined || value === null || String(value).trim() === '';
        return String(value) === tipo;
      })
      .map(entityFeature);
    renderAll();
  }

  function clearFilters() {
    filters = {};
    renderAll();
  }

  function syncMap(data) {
    DOMAINS.forEach(({ key, domain }) => {
      if (filters[key]) {
        GeoInsightMap.highlightZoneAnalysis(domain, filters[key]);
      } else {
        GeoInsightMap.showZoneAnalysisDomain(domain, allEntities(data, key));
      }
    });
  }

  function updatePill() {
    const pill = ui.$('zone-active-filter');
    if (!pill) return;
    const count = Object.keys(filters).length;
    if (count === 0) {
      pill.classList.add('hidden');
      return;
    }
    pill.innerHTML = `<span class="zone-active-chip">${count} ${count === 1 ? 'dominio en el mapa' : 'dominios en el mapa'}</span>`;
    const clear = document.createElement('button');
    clear.type = 'button';
    clear.className = 'zone-active-clear';
    clear.textContent = 'Restablecer en el mapa';
    clear.setAttribute('aria-label', 'Quitar los filtros y mostrar todas las entidades de la zona');
    clear.addEventListener('click', clearFilters);
    pill.appendChild(clear);
    pill.classList.remove('hidden');
  }

  function allEntities(data, key) {
    const breakdown = data[key];
    return breakdown.dataAvailable === false ? [] : (breakdown.entities || []);
  }

  function entityFeature(entity) {
    return {
      type: 'Feature',
      id: entity.id,
      properties: { ...(entity.attributes || {}), origin: entity.origin, _domain: entity.domain },
      geometry: entity.geometry
    };
  }

  function sortedEntries(distribution) {
    return Object.entries(distribution || {})
      .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'es'));
  }

  ui.$('zone-button').addEventListener('click', runZone);
  ui.$('zone-clear').addEventListener('click', () => {
    lastData = null;
    filters = {};
    GeoInsightMap.clearZoneAnalysis();
    result.innerHTML = '<span class="empty">Aún no hay un análisis.</span>';
  });
  ui.bindMapCoordinatePicker('zone-pick', 'zone');
  ui.$('zone-radius').addEventListener('keydown', (event) => {
    if (event.key === 'Enter') runZone();
  });
})();
