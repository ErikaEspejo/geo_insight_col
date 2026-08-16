// US3 — Consulta del contexto geocientífico de una coordenada (FR-009, FR-014).
// Presentación por secciones (Resultado / Contexto geológico / Elementos
// cercanos) con tarjetas compactas y distancias legibles. Solo descriptivo,
// sin inferencias de riesgo (FR-012).
(function () {
  const result = ui.$('context-result');

  const ICONS = {
    coordinate: '📍',
    unit: '▱',
    domain: '◈',
    fault: '〰',
    movement: '●',
    volcano: '▲'
  };

  function valueOf(entity, key) {
    const value = entity?.attributes?.[key];
    if (value === null || value === undefined || String(value).trim() === '') return null;
    return String(value);
  }

  function sectionTitle(title) {
    return `<h3 class="context-section-title">${ui.escapeHtml(title)}</h3>`;
  }

  function card(icon, label, body) {
    return `<article class="context-card">
      <header class="context-card-head"><span class="context-card-icon" aria-hidden="true">${icon}</span><span class="context-card-label">${ui.escapeHtml(label)}</span></header>
      ${body}
    </article>`;
  }

  function emptyBody(message) {
    return `<div class="context-card-value context-card-empty">${ui.escapeHtml(message)}</div>`;
  }

  function filledBody(value, meta) {
    const metaHtml = (meta || []).filter(Boolean).map((line) => `<div class="context-card-meta">${line}</div>`).join('');
    return `<div class="context-card-value">${ui.escapeHtml(value)}</div>${metaHtml}`;
  }

  function coordinateCard(lon, lat) {
    return card(ICONS.coordinate, 'Coordenada consultada',
      `<div class="context-card-value">${ui.escapeHtml(ui.pointText(lon, lat))}</div>
       <div class="context-card-meta">Ubicación consultada</div>`);
  }

  function unitCard(entity) {
    const value = valueOf(entity, 'SimboloUC') || valueOf(entity, 'UGIntegradas') || entity.id;
    const meta = [];
    const edad = valueOf(entity, 'Edad');
    if (edad) meta.push(`Edad: ${ui.escapeHtml(edad)}`);
    const descripcion = valueOf(entity, 'Descripcion');
    if (descripcion) meta.push(`<span class="context-card-desc">${ui.escapeHtml(descripcion)}</span>`);
    return card(ICONS.unit, 'Unidad geológica', filledBody(value, meta));
  }

  function domainCard(entity) {
    const value = valueOf(entity, 'NombreDT') || entity.id;
    const meta = [];
    const codigo = valueOf(entity, 'CodigoDT');
    if (codigo) meta.push(`Código: ${ui.escapeHtml(codigo)}`);
    return card(ICONS.domain, 'Dominio tectónico', filledBody(value, meta));
  }

  const nearestFault = (entity) => {
    const name = valueOf(entity, 'NombreFalla');
    const tipo = valueOf(entity, 'Tipo');
    const meta = [];
    if (tipo) meta.push(`Tipo: ${ui.escapeHtml(tipo)}`);
    if (name) meta.push(`ID: ${ui.escapeHtml(entity.id)}`);
    return { value: name || entity.id, meta };
  };

  const nearestMovement = (entity) => {
    const subtipo = valueOf(entity, 'SUBTIPO');
    const tipo = valueOf(entity, 'TIPO');
    const clasificacion = valueOf(entity, 'CLAS_MAPA');
    const meta = [];
    const parts = [];
    if (tipo && tipo !== subtipo && tipo !== clasificacion) parts.push(`Tipo: ${ui.escapeHtml(tipo)}`);
    if (clasificacion && clasificacion !== subtipo && clasificacion !== tipo) parts.push(`Clasificación: ${ui.escapeHtml(clasificacion)}`);
    if (parts.length) meta.push(parts.join(' · '));
    return { value: subtipo || tipo || clasificacion || entity.id, meta };
  };

  const nearestVolcano = (entity) => {
    const name = valueOf(entity, 'NombreVolcan');
    const altura = valueOf(entity, 'AlturaSobreNivelMar');
    const meta = [];
    if (altura) meta.push(`Altura: ${ui.escapeHtml(altura)} m`);
    if (name) meta.push(`ID: ${ui.escapeHtml(entity.id)}`);
    return { value: name || entity.id, meta };
  };

  function nearestCard(icon, label, entry, resolver) {
    if (!entry) return card(icon, label, emptyBody('No se encontró información disponible'));
    const info = resolver(entry.entity);
    const meta = [...info.meta, `${ui.formatDistance(entry.distanceMeters)} de la ubicación`];
    return card(icon, label, filledBody(info.value, meta));
  }

  function renderContext(data) {
    const units = data.geologicalUnits || [];
    const domains = data.tectonicDomains || [];
    let html = '<div class="context-result-body">';
    html += sectionTitle('Resultado');
    html += coordinateCard(data.coordinate.lon, data.coordinate.lat);
    html += sectionTitle('Contexto geológico');
    html += units.length
      ? units.map(unitCard).join('')
      : card(ICONS.unit, 'Unidad geológica', emptyBody('Sin información disponible'));
    html += domains.length
      ? domains.map(domainCard).join('')
      : card(ICONS.domain, 'Dominio tectónico', emptyBody('Sin información disponible'));
    html += sectionTitle('Elementos cercanos');
    html += nearestCard(ICONS.fault, 'Falla más cercana', data.nearestFault, nearestFault);
    html += nearestCard(ICONS.movement, 'Movimiento en masa más cercano', data.nearestMassMovement, nearestMovement);
    html += nearestCard(ICONS.volcano, 'Volcán más cercano', data.nearestVolcano, nearestVolcano);
    html += '</div>';
    result.innerHTML = html;
  }

  async function runContext(fromMapClick) {
    const point = ui.numberInput('context');
    if (!ui.validCoordinate(point)) {
      ui.showError(result, 'Ingrese longitud y latitud válidas.');
      return;
    }
    try {
      const data = await api.post('/api/context', { lon: point.lon, lat: point.lat });
      renderContext(data);
      GeoInsightMap.drawContext(data, { fromMapClick: !!fromMapClick });
    } catch (error) {
      ui.showError(result, error.message);
    }
  }

  window.GeoInsight = window.GeoInsight || {};
  window.GeoInsight.onMapClick = (lon, lat) => {
    ui.$('context-lon').value = lon;
    ui.$('context-lat').value = lat;
    runContext(true);
  };

  ui.$('context-button').addEventListener('click', () => runContext(false));
})();
