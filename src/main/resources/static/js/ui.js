// Utilidades de presentación compartidas por los módulos de la SPA.
const ui = {
  $(id) { return document.getElementById(id); },

  escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
  },

  formatMeters(meters) {
    if (meters === null || meters === undefined) return '—';
    return `${meters.toLocaleString('es-CO', { maximumFractionDigits: 0 })} m`;
  },

  pointText(lon, lat) {
    return `${lon.toLocaleString('es-CO', { maximumFractionDigits: 6 })}, ${lat.toLocaleString('es-CO', { maximumFractionDigits: 6 })}`;
  },

  entityName(entity) {
    const attributes = entity.attributes || {};
    return attributes.NombreFalla || attributes.NombreVolcan || attributes.NombreDT
      || attributes.SimboloUC || attributes.SUBTIPO || attributes.TIPO
      || attributes.CLAS_MAPA || attributes.ETIQUETA_M || entity.id;
  },

  numberInput(prefix) {
    return {
      lon: parseFloat(ui.$(`${prefix}-lon`).value),
      lat: parseFloat(ui.$(`${prefix}-lat`).value)
    };
  },

  validCoordinate(point) {
    return Number.isFinite(point.lon) && Number.isFinite(point.lat);
  },

  bindMapCoordinatePicker(buttonId, inputPrefix) {
    const button = ui.$(buttonId);
    button.addEventListener('click', () => {
      GeoInsightMap.startCoordinatePick(button, (lon, lat) => {
        ui.$(`${inputPrefix}-lon`).value = lon.toFixed(6);
        ui.$(`${inputPrefix}-lat`).value = lat.toFixed(6);
      });
    });
  },

  showError(container, message) {
    container.innerHTML = `<span class="error-text">${ui.escapeHtml(message)}</span>`;
  },

  distributionTable(distribution) {
    const entries = Object.entries(distribution || {});
    return entries.length === 0 ? '<li>Sin resultados.</li>'
      : `<ul>${entries.map(([value, count]) => `<li>${ui.escapeHtml(value)}: ${count}</li>`).join('')}</ul>`;
  }
};
