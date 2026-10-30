export const WorkCatalog = {
  codeFor(workType: string, itemName: string): string {
    const trade = workType.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 4).padEnd(4, 'X');
    const item = itemName.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 8).padEnd(3, 'X');
    // simple hash
    let hash = 0;
    const norm = itemName.trim().toLowerCase();
    for (let i = 0; i < norm.length; i++) {
      hash = (hash << 5) - hash + norm.charCodeAt(i);
      hash |= 0;
    }
    const suffix = Math.abs(hash).toString(16).toUpperCase().slice(-4).padStart(4, '0');
    return `${trade}-${item}-${suffix}`;
  },

  classify(contractorType: string, itemName: string): string {
    const name = itemName.trim().toLowerCase();
    const has = (...needles: string[]) => needles.some((n) => name.includes(n));

    if (has('excavat', 'earth', 'soil')) return 'Earthwork';
    if (has('rcc', 'pcc', 'concrete', 'reinforcement', 'shuttering', 'formwork')) return 'Concrete & Structure';
    if (has('brick', 'block', 'masonry')) return 'Masonry';
    if (has('plaster', 'putty', 'paint', 'primer', 'polish', 'texture')) return 'Finishes';
    if (has('tile', 'floor', 'marble', 'granite', 'cladding', 'skirting', 'dado', 'grout')) return 'Flooring & Cladding';
    if (has('pipe', 'drain', 'trap', 'basin', 'fitting', 'tank', 'wc ', 'pressure test')) return 'Plumbing Works';
    if (has('wire', 'cable', 'point', 'switch', 'earthing', 'distribution', 'conduit')) return 'Electrical Works';
    if (has('door', 'window', 'wardrobe', 'kitchen', 'wood', 'hardware')) return 'Joinery & Carpentry';
    if (has('waterproof', 'joint treatment')) return 'Waterproofing';
    if (has('duct', 'hvac', 'indoor unit', 'outdoor unit', 'diffuser', 'insulation')) return 'HVAC Works';
    if (has('fire', 'sprinkler', 'hydrant', 'hose reel')) return 'Fire Protection';
    if (has('steel', 'railing', 'grill', 'weld', 'truss', 'fabricat')) return 'Fabrication';
    if (has('lawn', 'plant', 'paver', 'kerb', 'irrigation')) return 'Landscaping';

    const cleanType = contractorType.trim() || 'General';
    return cleanType.endsWith('Works') ? cleanType : `${cleanType} Works`;
  },
};

export const ExportHelper = {
  downloadCSV(filename: string, content: string) {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  },

  downloadJSON(filename: string, data: unknown) {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  },
};
