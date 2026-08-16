// US4 — Análisis descriptivo de una zona (FR-010, FR-012). Solo indicadores,
// sin conclusiones de riesgo.
(function () {
  const result = ui.$('zone-result');

  async function runZone() {
    const point = ui.numberInput('zone');
    const radius = parseFloat(ui.$('zone-radius').value);
    if (!ui.validCoordinate(point) || !Number.isFinite(radius) || radius <= 0) {
      ui.showError(result, 'Ingrese coordenadas y un radio válido en metros.');
      return;
    }
    try {
      const data = await api.post('/api/zones/analyze', { lon: point.lon, lat: point.lat, radiusMeters: radius });
      GeoInsightMap.clearZoneCircles();
      GeoInsightMap.drawZoneCircle(point.lon, point.lat, radius);
      renderZone(data);
    } catch (error) {
      ui.showError(result, error.message);
    }
  }

  function renderZone(data) {
    result.innerHTML = `
      <p><strong>Movimientos en masa:</strong> ${data.massMovements.count}</p>
      <p><strong>Por tipo:</strong></p>
      ${ui.distributionTable(data.massMovements.byTipo)}
      <p><strong>Por subtipo:</strong></p>
      ${ui.distributionTable(data.massMovements.bySubtipo)}
      <p><strong>Por clasificación en mapa:</strong></p>
      ${ui.distributionTable(data.massMovements.byClasMapa)}
      <p><strong>Fallas:</strong> ${data.faults.count}</p>
      <p><strong>Unidades geológicas:</strong> ${data.geologicalUnits.count}</p>
      <p><strong>Dominios tectónicos:</strong> ${data.tectonicDomains.count}</p>
      <p><strong>Volcanes:</strong> ${data.volcanoes.count}</p>`;
  }

  ui.$('zone-button').addEventListener('click', runZone);
  ui.bindMapCoordinatePicker('zone-pick', 'zone');
  ui.$('zone-radius').addEventListener('keydown', (event) => {
    if (event.key === 'Enter') runZone();
  });
})();
