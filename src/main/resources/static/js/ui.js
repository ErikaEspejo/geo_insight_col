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

  formatNumber(value) {
    return Number(value).toLocaleString('es-CO');
  },

  formatPercent(part, total) {
    if (!Number.isFinite(total) || total <= 0) return '0 %';
    const percent = (part / total) * 100;
    return `${percent.toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} %`;
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
  }
};
