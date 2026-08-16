// US6 — Gestión de entidades GEOINSIGHT con campos y geometrías controlados.
(function () {
  const state = { editingId: null, geometry: null };
  const CONTROLLED_ATTRIBUTES = new Set(['TIPO', 'SUBTIPO', 'CLAS_MAPA', 'Tipo', 'Edad']);

  function selectedLayer() {
    return window.GeoInsight.layers().find((layer) => layer.domain === ui.$('admin-domain').value);
  }

  function adminPayload() {
    const layer = selectedLayer();
    if (!state.geometry) throw new Error('Dibuja la geometría de la entidad en el mapa.');
    const attributes = {};
    ui.$('admin-fields').querySelectorAll('[data-attribute]').forEach((input) => {
      const value = input.value.trim();
      if (value === '') return;
      const attribute = input.dataset.attribute;
      const type = layer.editableAttributeTypes?.[attribute] || 'TEXT';
      if (type === 'INTEGER') {
        if (!/^-?\d+$/.test(value)) throw new Error(`${attribute} debe ser un número entero.`);
        attributes[attribute] = Number.parseInt(value, 10);
      } else if (type === 'DECIMAL') {
        const numericValue = Number(value);
        if (!Number.isFinite(numericValue)) throw new Error(`${attribute} debe ser numérico.`);
        attributes[attribute] = numericValue;
      } else if (type === 'BOOLEAN') {
        attributes[attribute] = value === 'true';
      } else {
        attributes[attribute] = value;
      }
    });
    return { domain: layer.domain, geometry: state.geometry, attributes };
  }

  function renderFields(values = {}) {
    const layer = selectedLayer();
    const required = new Set(layer.requiredAttributes || []);
    const valueOptions = new Map((layer.filterableAttributes || [])
      .map((attribute) => [attribute.name, attribute.values || []]));
    const container = ui.$('admin-fields');
    container.innerHTML = '<h3>Datos de la entidad</h3>';
    (layer.editableAttributes || []).forEach((attribute) => {
      const field = document.createElement('div');
      field.className = 'admin-field';
      const requiredMark = required.has(attribute) ? ' <span aria-label="obligatorio">*</span>' : '';
      const attributeType = layer.editableAttributeTypes?.[attribute] || 'TEXT';
      const typeLabel = { INTEGER: 'Entero', DECIMAL: 'Decimal', BOOLEAN: 'Sí/No', TEXT: 'Texto' }[attributeType];
      const options = CONTROLLED_ATTRIBUTES.has(attribute) ? valueOptions.get(attribute) : null;
      field.innerHTML = `<label for="admin-field-${ui.escapeHtml(attribute)}">${ui.escapeHtml(attribute)}${requiredMark}<small>${typeLabel}</small></label>`;
      let control;
      if (options?.length) {
        control = document.createElement('select');
        control.innerHTML = '<option value="">Seleccione un valor</option>';
        options.forEach((optionValue) => {
          const option = document.createElement('option');
          option.value = optionValue;
          option.textContent = optionValue;
          control.appendChild(option);
        });
        const currentValue = values[attribute];
        if (currentValue !== undefined && currentValue !== null && currentValue !== ''
            && !options.includes(String(currentValue))) {
          const currentOption = document.createElement('option');
          currentOption.value = currentValue;
          currentOption.textContent = `${currentValue} (valor existente)`;
          control.appendChild(currentOption);
        }
      } else {
        control = document.createElement('input');
        control.type = attributeType === 'INTEGER' || attributeType === 'DECIMAL' ? 'number' : 'text';
        if (attributeType === 'INTEGER') control.step = '1';
        if (attributeType === 'DECIMAL') control.step = 'any';
        control.autocomplete = 'off';
      }
      control.id = `admin-field-${attribute}`;
      control.dataset.attribute = attribute;
      control.value = values[attribute] ?? '';
      field.appendChild(control);
      container.appendChild(field);
    });
    if (!(layer.editableAttributes || []).length) {
      container.insertAdjacentHTML('beforeend', '<p class="error-text">No hay campos descriptivos disponibles para este dominio.</p>');
    }
    ui.$('admin-geometry-hint').textContent = drawingHint(layer.geometryType);
  }

  function drawingHint(type) {
    if (type === 'Point') return 'Haz clic una vez sobre el mapa para ubicar el punto.';
    if (type === 'LineString') return 'Agrega al menos dos vértices y finaliza el dibujo.';
    return 'Agrega al menos tres vértices y finaliza el polígono.';
  }

  function setGeometry(geometry) {
    state.geometry = geometry;
    const type = geometry?.type;
    ui.$('admin-geometry-status').className = 'admin-geometry-status ready';
    ui.$('admin-geometry-status').textContent = `${type} listo para guardar.`;
    ui.$('admin-finish-draw').classList.add('hidden');
  }

  function confirmDeletion(entityName) {
    return new Promise((resolve) => {
      const modal = ui.$('delete-modal');
      const cancelButton = ui.$('delete-modal-cancel');
      const confirmButton = ui.$('delete-modal-confirm');
      const backdrop = modal.querySelector('[data-modal-cancel]');
      ui.$('delete-modal-message').textContent = `¿Deseas eliminar “${entityName}”?`;
      modal.classList.remove('hidden');
      document.body.classList.add('modal-open');

      const close = (confirmed) => {
        modal.classList.add('hidden');
        document.body.classList.remove('modal-open');
        cancelButton.removeEventListener('click', cancel);
        confirmButton.removeEventListener('click', confirm);
        backdrop.removeEventListener('click', cancel);
        document.removeEventListener('keydown', onKeydown);
        resolve(confirmed);
      };
      const cancel = () => close(false);
      const confirm = () => close(true);
      const onKeydown = (event) => {
        if (event.key === 'Escape') cancel();
      };
      cancelButton.addEventListener('click', cancel);
      confirmButton.addEventListener('click', confirm);
      backdrop.addEventListener('click', cancel);
      document.addEventListener('keydown', onKeydown);
      cancelButton.focus();
    });
  }

  function startDrawing() {
    if (ui.$('admin-draw').classList.contains('active')) {
      GeoInsightMap.cancelGeometryDraw(true);
      ui.$('admin-finish-draw').classList.add('hidden');
      ui.$('admin-geometry-status').className = 'admin-geometry-status empty';
      ui.$('admin-geometry-status').textContent = 'Dibujo cancelado.';
      return;
    }
    const layer = selectedLayer();
    state.geometry = null;
    ui.$('admin-geometry-status').className = 'admin-geometry-status empty';
    ui.$('admin-geometry-status').textContent = 'Dibujo en curso…';
    ui.$('admin-finish-draw').classList.toggle('hidden', layer.geometryType === 'Point');
    GeoInsightMap.startGeometryDraw(layer.geometryType, ui.$('admin-draw'), setGeometry);
  }

  function resetAdminForm() {
    state.editingId = null;
    state.geometry = null;
    GeoInsightMap.cancelGeometryDraw(true);
    renderFields();
    ui.$('admin-create').textContent = 'Crear entidad';
    ui.$('admin-domain').disabled = false;
    ui.$('admin-finish-draw').classList.add('hidden');
    ui.$('admin-geometry-status').className = 'admin-geometry-status empty';
    ui.$('admin-geometry-status').textContent = 'Aún no se ha dibujado una geometría.';
  }

  async function loadAdminList() {
    const list = ui.$('admin-list');
    const entities = await api.get('/api/admin/entities');
    GeoInsightMap.renderAdminEntities(entities);
    list.innerHTML = '<div class="admin-list-heading"><span class="eyebrow">MIS ENTIDADES</span><strong>Entidades GeoInsight</strong><small>' + entities.length + ' creadas</small></div>';
    if (entities.length === 0) {
      list.insertAdjacentHTML('beforeend', '<span class="empty">No hay entidades GEOINSIGHT.</span>');
      return;
    }
    entities.forEach((entity) => {
      const card = document.createElement('div');
      card.className = 'admin-card';
      card.style.setProperty('--admin-domain-color', GeoInsightMap.domainColor(entity.domain));
      card.innerHTML = `<div class="admin-card-title"><span class="admin-domain-dot"></span><strong>${ui.escapeHtml(ui.entityName(entity))}</strong></div><small>${ui.escapeHtml(entity.domain)}</small><div class="admin-actions"><button class="secondary" type="button">Editar</button><button class="danger" type="button">Eliminar</button></div>`;
      card.querySelector('.secondary').addEventListener('click', () => editAdminEntity(entity));
      card.querySelector('.danger').addEventListener('click', async () => {
        const entityName = ui.entityName(entity);
        const confirmed = await confirmDeletion(entityName);
        if (!confirmed) return;
        try {
          await api.del(`/api/admin/entities/${entity.id}`);
          if (state.editingId === entity.id) resetAdminForm();
          await loadAdminList();
        } catch (error) {
          ui.showError(list, error.message);
        }
      });
      list.appendChild(card);
    });
  }

  function editAdminEntity(entity) {
    state.editingId = entity.id;
    state.geometry = entity.geometry;
    ui.$('admin-domain').value = entity.domain;
    ui.$('admin-domain').disabled = true;
    renderFields(entity.attributes || {});
    GeoInsightMap.renderAdminGeometry(entity.geometry);
    setGeometry(entity.geometry);
    ui.$('admin-create').textContent = 'Guardar cambios';
  }

  async function saveEntity() {
    const feedback = ui.$('admin-list');
    try {
      const payload = adminPayload();
      if (state.editingId) await api.put(`/api/admin/entities/${state.editingId}`, payload);
      else await api.post('/api/admin/entities', payload);
      resetAdminForm();
      await loadAdminList();
    } catch (error) {
      ui.showError(feedback, error.message);
    }
  }

  async function initAdmin() {
    const me = window.GeoInsight && window.GeoInsight.me;
    if (!me || me.role !== 'ADMIN') return;
    ui.$('nav-admin').classList.remove('hidden');
    const select = ui.$('admin-domain');
    window.GeoInsight.layers().forEach((layer) => {
      const option = document.createElement('option');
      option.value = layer.domain;
      option.textContent = layer.name;
      select.appendChild(option);
    });
    select.addEventListener('change', resetAdminForm);
    ui.$('admin-draw').addEventListener('click', startDrawing);
    ui.$('admin-finish-draw').addEventListener('click', () => {
      if (!GeoInsightMap.finishGeometryDraw()) {
        ui.$('admin-geometry-status').className = 'admin-geometry-status error-text';
        ui.$('admin-geometry-status').textContent = drawingHint(selectedLayer().geometryType);
      }
    });
    ui.$('admin-create').addEventListener('click', saveEntity);
    ui.$('admin-clear').addEventListener('click', resetAdminForm);
    resetAdminForm();
    await loadAdminList();
  }

  const layersReady = new Promise((resolve) => {
    const check = () => {
      if (window.GeoInsight && window.GeoInsight.layers && window.GeoInsight.layers().length > 0) resolve();
      else setTimeout(check, 50);
    };
    check();
  });
  layersReady.then(initAdmin);
})();
