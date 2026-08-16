// US5 — Comparación descriptiva de dos zonas con indicadores equivalentes.
(function () {
  const result = ui.$('compare-result');

  async function runCompare() {
    const a = ui.numberInput('compare-a');
    const b = ui.numberInput('compare-b');
    const radius = parseFloat(ui.$('compare-radius').value);
    if (!ui.validCoordinate(a) || !ui.validCoordinate(b) || !Number.isFinite(radius) || radius <= 0) {
      ui.showError(result, 'Ingrese las dos coordenadas y un radio común válido.');
      return;
    }
    const button = ui.$('compare-button');
    button.disabled = true;
    button.textContent = 'Comparando zonas…';
    result.innerHTML = '<div class="skeleton">Calculando indicadores equivalentes…</div>';
    try {
      const data = await api.post('/api/zones/compare', {
        zoneA: { lon: a.lon, lat: a.lat },
        zoneB: { lon: b.lon, lat: b.lat },
        radiusMeters: radius
      });
      GeoInsightMap.drawComparisonZones(data.zoneA.zone, data.zoneB.zone);
      renderCompare(data);
    } catch (error) {
      ui.showError(result, `No fue posible realizar la comparación: ${error.message}`);
    } finally {
      button.disabled = false;
      button.textContent = 'Comparar zonas';
    }
  }

  function renderCompare(data) {
    const a = data.zoneA;
    const b = data.zoneB;
    result.innerHTML = `
      <div class="compare-title"><span class="eyebrow">COMPARACIÓN DE ZONAS</span><h3>Caracterización geocientífica</h3></div>
      <div class="compare-zones">${zoneHeader('A', a.zone)}<div class="compare-vs">VS</div>${zoneHeader('B', b.zone)}</div>
      <div class="compare-observations"><strong>Observaciones descriptivas</strong>${observations(a, b)}</div>
      <div class="compare-sections">
        ${movementSection(a, b)}
        ${nearestSection('Fallas geológicas', 'Fallas dentro o intersectadas por la zona', a.faults, b.faults, a.nearestFault, b.nearestFault)}
        ${geologySection(a, b)}
        ${tectonicSection(a, b)}
        ${nearestSection('Volcanismo', 'Volcanes dentro de la zona', a.volcanoes, b.volcanoes, a.nearestVolcano, b.nearestVolcano)}
      </div>
      ${summaryTable(a, b)}`;
  }

  function zoneHeader(label, zone) {
    return `<div class="compare-zone compare-zone-${label.toLowerCase()}"><span class="compare-zone-letter">${label}</span><div><strong>Zona ${label}</strong><small>Coordenadas: ${ui.escapeHtml(ui.pointText(zone.lon, zone.lat))}<br>Radio: ${ui.escapeHtml(ui.formatMeters(zone.radiusMeters))}</small></div></div>`;
  }

  function movementSection(a, b) {
    return section('Movimientos en masa',
      side(domainCount(a.massMovements, 'registros dentro de la zona'), [
        metric('Tipo predominante', predominant(a.massMovements)),
        nearestMetric('Movimiento más cercano', a.massMovements.dataAvailable === false ? undefined : a.nearestMassMovement)
      ]),
      side(domainCount(b.massMovements, 'registros dentro de la zona'), [
        metric('Tipo predominante', predominant(b.massMovements)),
        nearestMetric('Movimiento más cercano', b.massMovements.dataAvailable === false ? undefined : b.nearestMassMovement)
      ]));
  }

  function geologySection(a, b) {
    return section('Geología',
      side(domainCount(a.geologicalUnits, 'unidades intersectadas'), [
        entityListMetric('Presentes en la zona', availableEntities(a.geologicalUnits, a.geologicalUnits.entities), geologicalLabel),
        entityListMetric('Edades disponibles', availableEntities(a.geologicalUnits, uniqueAttributes(a.geologicalUnits.entities, 'Edad')))
      ]),
      side(domainCount(b.geologicalUnits, 'unidades intersectadas'), [
        entityListMetric('Presentes en la zona', availableEntities(b.geologicalUnits, b.geologicalUnits.entities), geologicalLabel),
        entityListMetric('Edades disponibles', availableEntities(b.geologicalUnits, uniqueAttributes(b.geologicalUnits.entities, 'Edad')))
      ]));
  }

  function tectonicSection(a, b) {
    return section('Tectónica',
      side(domainCount(a.tectonicDomains, 'dominios intersectados'), [
        entityListMetric('Presentes en la zona', availableEntities(a.tectonicDomains, a.tectonicDomains.entities), tectonicLabel)
      ]),
      side(domainCount(b.tectonicDomains, 'dominios intersectados'), [
        entityListMetric('Presentes en la zona', availableEntities(b.tectonicDomains, b.tectonicDomains.entities), tectonicLabel)
      ]));
  }

  function nearestSection(title, countLabel, aBreakdown, bBreakdown, aNearest, bNearest) {
    const nearestLabel = title === 'Volcanismo' ? 'Volcán más cercano' : 'Falla más cercana';
    return section(title,
      side(domainCount(aBreakdown, countLabel), [nearestMetric(nearestLabel, aBreakdown.dataAvailable === false ? undefined : aNearest)]),
      side(domainCount(bBreakdown, countLabel), [nearestMetric(nearestLabel, bBreakdown.dataAvailable === false ? undefined : bNearest)]));
  }

  function section(title, sideA, sideB) {
    return `<section class="compare-domain"><h4>${ui.escapeHtml(title)}</h4><div class="compare-domain-grid"><div data-zone="A">${sideA}</div><div data-zone="B">${sideB}</div></div></section>`;
  }

  function side(primary, metrics) {
    return `<div class="compare-primary">${primary}</div>${metrics.join('')}`;
  }

  function domainCount(breakdown, label) {
    if (!breakdown || breakdown.dataAvailable === false) return '<span class="data-unavailable">Información no disponible</span>';
    return `<strong>${breakdown.count}</strong> ${ui.escapeHtml(label)}`;
  }

  function metric(label, value) {
    return `<div class="compare-metric"><span>${ui.escapeHtml(label)}</span><strong>${ui.escapeHtml(value)}</strong></div>`;
  }

  function nearestMetric(label, nearest) {
    if (nearest === undefined) return metric(label, 'Información no disponible');
    if (nearest === null) return metric(label, 'Sin registros dentro del radio');
    return `<div class="compare-metric"><span>${ui.escapeHtml(label)}</span><strong>${ui.escapeHtml(ui.entityName(nearest.entity))}</strong><small>Distancia desde el centro: ${ui.escapeHtml(ui.formatMeters(nearest.distanceMeters))}</small></div>`;
  }

  function entityListMetric(label, entities, formatter) {
    if (entities === undefined) return metric(label, 'Información no disponible');
    const values = (entities || []).map((entity) => typeof entity === 'string' ? entity : (formatter || ui.entityName)(entity)).filter(Boolean);
    return metric(label, values.length ? [...new Set(values)].join(' · ') : 'Sin registros');
  }

  function availableEntities(breakdown, entities) {
    return breakdown?.dataAvailable === false ? undefined : entities;
  }

  function predominant(breakdown) {
    if (!breakdown || breakdown.dataAvailable === false) return 'Información no disponible';
    if (breakdown.count === 0) return 'No aplica (0 registros)';
    const entries = Object.entries(breakdown.byTipo || {});
    if (!entries.length) return 'Atributo sin valor';
    const maximum = Math.max(...entries.map(([, count]) => count));
    const leaders = entries.filter(([, count]) => count === maximum).map(([name]) => name);
    return leaders.length === 1 ? `${leaders[0]} (${maximum})` : `Empate: ${leaders.join(', ')} (${maximum} cada uno)`;
  }

  function uniqueAttributes(entities, key) {
    return [...new Set((entities || []).map((entity) => entity.attributes?.[key]).filter((value) => value !== null && value !== undefined && String(value).trim() !== '').map(String))];
  }

  function geologicalLabel(entity) {
    const attributes = entity.attributes || {};
    const name = attributes.UGIntegradas || attributes.SimboloUC;
    if (!name) return 'Atributo sin valor';
    return attributes.Edad ? `${name} — ${attributes.Edad}` : String(name);
  }

  function tectonicLabel(entity) {
    const attributes = entity.attributes || {};
    if (!attributes.NombreDT && !attributes.CodigoDT) return 'Atributo sin valor';
    return [attributes.NombreDT, attributes.CodigoDT].filter(Boolean).join(' · ');
  }

  function observations(a, b) {
    const items = [];
    addCountObservation(items, a.massMovements, b.massMovements, 'movimientos en masa registrados');
    addCountObservation(items, a.faults, b.faults, 'fallas presentes o próximas');
    if (available(a.tectonicDomains, b.tectonicDomains) && a.tectonicDomains.count === 1 && b.tectonicDomains.count === 1) items.push('Ambas zonas intersectan un dominio tectónico.');
    if (available(a.volcanoes, b.volcanoes) && a.volcanoes.count === 0 && b.volcanoes.count === 0) items.push('No se registran volcanes dentro del radio analizado en ninguna de las zonas.');
    addDistanceObservation(items, a.nearestFault, b.nearestFault, 'La falla más cercana');
    return items.length ? `<ul>${items.map((item) => `<li>${ui.escapeHtml(item)}</li>`).join('')}</ul>` : '<p>No hay diferencias descriptivas destacadas con los indicadores disponibles.</p>';
  }

  function available(a, b) {
    return a?.dataAvailable !== false && b?.dataAvailable !== false;
  }

  function addCountObservation(items, a, b, noun) {
    if (!available(a, b) || a.count === b.count) return;
    const zone = a.count > b.count ? 'A' : 'B';
    items.push(`Zona ${zone} contiene más ${noun} que Zona ${zone === 'A' ? 'B' : 'A'}.`);
  }

  function addDistanceObservation(items, a, b, subject) {
    if (!a || !b || a.distanceMeters === b.distanceMeters) return;
    const zone = a.distanceMeters < b.distanceMeters ? 'A' : 'B';
    items.push(`${subject} se encuentra a menor distancia del centro de Zona ${zone}.`);
  }

  function summaryTable(a, b) {
    const rows = [
      ['Movimientos en masa', a.massMovements, b.massMovements],
      ['Fallas geológicas', a.faults, b.faults],
      ['Unidades geológicas', a.geologicalUnits, b.geologicalUnits],
      ['Dominios tectónicos', a.tectonicDomains, b.tectonicDomains],
      ['Volcanes dentro de la zona', a.volcanoes, b.volcanoes]
    ];
    const value = (entry) => entry?.dataAvailable === false ? 'No disponible' : String(entry?.count ?? 'No disponible');
    return `<details class="compare-summary"><summary>Ver tabla resumen</summary><table class="attr-table"><thead><tr><th>Dominio</th><th>A</th><th>B</th></tr></thead><tbody>${rows.map(([label, left, right]) => `<tr><td>${ui.escapeHtml(label)}</td><td>${ui.escapeHtml(value(left))}</td><td>${ui.escapeHtml(value(right))}</td></tr>`).join('')}</tbody></table></details>`;
  }

  ui.$('compare-button').addEventListener('click', runCompare);
  ui.$('compare-clear').addEventListener('click', () => {
    GeoInsightMap.clearZoneCircles();
    result.innerHTML = '<span class="empty">Aún no hay una comparación.</span>';
  });
  ui.bindMapCoordinatePicker('compare-a-pick', 'compare-a');
  ui.bindMapCoordinatePicker('compare-b-pick', 'compare-b');
  ui.$('compare-radius').addEventListener('keydown', (event) => {
    if (event.key === 'Enter') runCompare();
  });
})();
