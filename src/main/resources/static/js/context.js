// US3 — Consulta del contexto geocientífico de una coordenada (FR-009, FR-014).
// Se activa con el botón o haciendo clic en el mapa.
(function () {
  const result = ui.$('context-result');

  async function runContext() {
    const point = ui.numberInput('context');
    if (!ui.validCoordinate(point)) {
      ui.showError(result, 'Ingrese longitud y latitud válidas.');
      return;
    }
    try {
      const data = await api.post('/api/context', { lon: point.lon, lat: point.lat });
      renderContext(data);
    } catch (error) {
      ui.showError(result, error.message);
    }
  }

  function renderContext(data) {
    const units = data.geologicalUnits.map((e) => `<li>${ui.escapeHtml(ui.entityName(e))}</li>`).join('');
    const domains = data.tectonicDomains.map((e) => `<li>${ui.escapeHtml(ui.entityName(e))}</li>`).join('');

    function nearest(label, entry) {
      if (!entry) return `<p><strong>${ui.escapeHtml(label)}:</strong> sin resultado.</p>`;
      return `<p><strong>${ui.escapeHtml(label)}:</strong> ${ui.escapeHtml(ui.entityName(entry.entity))}
        a ${ui.escapeHtml(ui.formatMeters(entry.distanceMeters))}.</p>`;
    }

    result.innerHTML = `
      <p>Coordenada: ${ui.escapeHtml(ui.pointText(data.coordinate.lon, data.coordinate.lat))}</p>
      <p><strong>Unidades geológicas:</strong></p>
      <ul>${units || '<li>Sin resultado.</li>'}</ul>
      <p><strong>Dominios tectónicos:</strong></p>
      <ul>${domains || '<li>Sin resultado.</li>'}</ul>
      ${nearest('Falla más cercana', data.nearestFault)}
      ${nearest('Movimiento en masa más cercano', data.nearestMassMovement)}
      ${nearest('Volcán más cercano', data.nearestVolcano)}`;
  }

  window.GeoInsight = window.GeoInsight || {};
  window.GeoInsight.onMapClick = (lon, lat) => {
    ui.$('context-lon').value = lon;
    ui.$('context-lat').value = lat;
    runContext();
  };

  ui.$('context-button').addEventListener('click', runContext);
})();
