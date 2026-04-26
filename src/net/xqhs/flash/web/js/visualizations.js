import { sendGlobalCommandWithPayload } from './events.js';

export const visualizationsConfig = [];
const STORAGE_KEY = 'flash_mas_visualizations';

export function saveConfig() {
    const configToSave = visualizationsConfig.map(viz => {
        const copy = { ...viz };
        delete copy.chartInstance;
        delete copy.agentDataMap;
        delete copy.agentLinksMap;
        return copy;
    });
    localStorage.setItem(STORAGE_KEY, JSON.stringify(configToSave));
}

function loadConfig() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
        try { return JSON.parse(saved); }
        catch (e) { console.error("Failed to parse saved configs", e); }
    }
    return [];
}

export function initVisualizations() {
    const dashboard = document.getElementById('viz-dashboard');
    if (!dashboard) return;

    const loadedConfig = loadConfig();
    if (loadedConfig && loadedConfig.length > 0) {
        visualizationsConfig.push(...loadedConfig);

        visualizationsConfig.forEach(viz => {
            createVisualizationWidget(viz, dashboard);

            let metricName = viz.xAxisProperty;
            if (viz.type === 'scatter' || viz.type === 'heatmap') {
                metricName = `${viz.xAxisProperty},${viz.yAxisProperty}`;
                if (viz.type === 'heatmap' && viz.zAxisProperty) {
                    metricName += `,${viz.zAxisProperty}`;
                }
            }
            sendGlobalCommandWithPayload("deployment", {
                command: "SUBSCRIBE_METRICS",
                category: viz.targetCategory,
                metric: metricName
            });
        });
    }

    const headerControls = document.getElementById('viz-header-controls');
    if (headerControls && !document.getElementById('btn-clear-dash')) {
        const btnClear = document.createElement('button');
        btnClear.id = 'btn-clear-dash';
        btnClear.innerText = "Clear Dashboard";
        btnClear.style = "background:#f44336; color:white; border:none; padding:5px 15px; border-radius:4px; cursor:pointer; font-weight:bold; font-size:0.9rem; margin-right:10px;";
        btnClear.onclick = (e) => {
            e.preventDefault();
            if(confirm("Are you sure you want to delete all visualizations?")) {
                localStorage.removeItem(STORAGE_KEY);
                visualizationsConfig.length = 0;
                dashboard.innerHTML = '';
            }
        };

        const btnExport = document.createElement('button');
        btnExport.id = 'btn-export-yaml';
        btnExport.innerText = "Export YAML";
        btnExport.style = "background:#ff9800; color:white; border:none; padding:5px 15px; border-radius:4px; cursor:pointer; font-weight:bold; font-size:0.9rem; margin-right:10px;";
        btnExport.onclick = (e) => {
            e.preventDefault();
            const configToSave = visualizationsConfig.map(viz => {
                const copy = { ...viz };
                delete copy.chartInstance;
                delete copy.agentDataMap;
                delete copy.agentLinksMap;
                return copy;
            });
            const yamlStr = jsyaml.dump(configToSave);
            const blob = new Blob([yamlStr], { type: 'text/yaml' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'dashboard_config.yml';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        };

        const btnImport = document.createElement('button');
        btnImport.id = 'btn-import-yaml';
        btnImport.innerText = "Import YAML";
        btnImport.style = "background:#2196F3; color:white; border:none; padding:5px 15px; border-radius:4px; cursor:pointer; font-weight:bold; font-size:0.9rem; margin-right:10px;";
        btnImport.onclick = (e) => {
            e.preventDefault();
            document.getElementById('import-yaml-file').click();
        };

        const btnAdd = document.getElementById('btn-open-viz-modal');
        if (btnAdd) {
            headerControls.insertBefore(btnClear, btnAdd);
            headerControls.insertBefore(btnExport, btnAdd);
            headerControls.insertBefore(btnImport, btnAdd);
        }
    }

    const fileInput = document.getElementById('import-yaml-file');
    if (fileInput) {
        fileInput.onchange = (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = (ev) => {
                try {
                    const parsedConfig = jsyaml.load(ev.target.result);
                    if (Array.isArray(parsedConfig)) {
                        localStorage.setItem(STORAGE_KEY, JSON.stringify(parsedConfig));
                        window.location.reload();
                    } else {
                        alert("Invalid YAML configuration format.");
                    }
                } catch (err) {
                    alert("Error parsing YAML file: " + err.message);
                }
            };
            reader.readAsText(file);
            e.target.value = '';
        };
    }

    window.addEventListener('resize', () => {
        visualizationsConfig.forEach(viz => {
            if (viz.chartInstance) viz.chartInstance.resize();
        });
    });

    dashboard.addEventListener('dragover', e => {
        e.preventDefault();
        const draggingCard = document.querySelector('.dragging');
        const targetCard = e.target.closest('.viz-card');

        if (draggingCard && targetCard && draggingCard !== targetCard) {
            const box = targetCard.getBoundingClientRect();
            const isBefore = (e.clientX - box.left) < box.width / 2;

            if (isBefore) {
                targetCard.parentNode.insertBefore(draggingCard, targetCard);
            } else {
                targetCard.parentNode.insertBefore(draggingCard, targetCard.nextSibling);
            }
        }
    });

    dashboard.addEventListener('drop', e => {
        e.preventDefault();
        const newOrderIds = Array.from(dashboard.querySelectorAll('.viz-card')).map(card => card.dataset.id);
        visualizationsConfig.sort((a, b) => newOrderIds.indexOf(a.id) - newOrderIds.indexOf(b.id));
        saveConfig();
        visualizationsConfig.forEach(v => { if (v.chartInstance) v.chartInstance.resize(); });
    });

    setupVizModal();
}

export function createVisualizationWidget(viz, containerElement) {
    if (!containerElement) {
        containerElement = document.getElementById('viz-dashboard');
    }

    viz.agentDataMap = new Map();

    const card = document.createElement('div');
    card.className = `viz-card ${viz.widthClass || 'w-100'} ${viz.heightClass || 'h-1'}`;
    card.setAttribute('draggable', 'true');
    card.dataset.id = viz.id;

    card.innerHTML = `
        <div class="viz-card-header">
            <h3>${viz.title} <span style="font-size:0.7em; color:#888; font-weight:normal;">(${viz.targetCategory})</span></h3>
            <div class="viz-controls">
                <select class="resize-w" title="Width">
                    <option value="w-25" ${viz.widthClass === 'w-25' ? 'selected' : ''}>25%</option>
                    <option value="w-33" ${viz.widthClass === 'w-33' ? 'selected' : ''}>33%</option>
                    <option value="w-50" ${viz.widthClass === 'w-50' ? 'selected' : ''}>50%</option>
                    <option value="w-66" ${viz.widthClass === 'w-66' ? 'selected' : ''}>66%</option>
                    <option value="w-75" ${viz.widthClass === 'w-75' ? 'selected' : ''}>75%</option>
                    <option value="w-100" ${viz.widthClass === 'w-100' ? 'selected' : ''}>100%</option>
                </select>
                <select class="resize-h" title="Height">
                    <option value="h-1" ${viz.heightClass === 'h-1' ? 'selected' : ''}>1x</option>
                    <option value="h-2" ${viz.heightClass === 'h-2' ? 'selected' : ''}>2x</option>
                    <option value="h-3" ${viz.heightClass === 'h-3' ? 'selected' : ''}>3x</option>
                </select>
                <button class="remove-viz" title="Remove Plot">X</button>
            </div>
        </div>
        <div id="viz-canvas-${viz.id}" class="viz-canvas"></div>
    `;
    containerElement.appendChild(card);

    card.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('text/plain', viz.id);
        setTimeout(() => card.classList.add('dragging'), 0);
    });
    card.addEventListener('dragend', () => {
        card.classList.remove('dragging');
    });

    card.querySelector('.resize-w').addEventListener('change', (e) => {
        card.classList.remove('w-25', 'w-33', 'w-50', 'w-66', 'w-75', 'w-100');
        card.classList.add(e.target.value);
        viz.widthClass = e.target.value;
        saveConfig();
        if (viz.chartInstance) viz.chartInstance.resize();
    });
    card.querySelector('.resize-h').addEventListener('change', (e) => {
        card.classList.remove('h-1', 'h-2', 'h-3');
        card.classList.add(e.target.value);
        viz.heightClass = e.target.value;
        saveConfig();
        if (viz.chartInstance) viz.chartInstance.resize();
    });
    card.querySelector('.remove-viz').addEventListener('click', () => {
        if(confirm("Remove this plot?")) {
            const idx = visualizationsConfig.findIndex(v => v.id === viz.id);
            if (idx > -1) visualizationsConfig.splice(idx, 1);
            card.remove();
            saveConfig();
        }
    });

    const chartDom = document.getElementById(`viz-canvas-${viz.id}`);
    const myChart = echarts.init(chartDom);
    viz.chartInstance = myChart;

    let option = {};

    if (viz.type === 'scatter') {
        option = {
            tooltip: {
                trigger: 'item',
                formatter: function (params) {
                    return `Agent: ${params.value[2]}<br/>X: ${params.value[0]}<br/>Y: ${params.value[1]}`;
                }
            },
            toolbox: { feature: { dataZoom: {}, restore: {}, saveAsImage: {} } },
            dataZoom: [
                { type: 'slider', xAxisIndex: 0, filterMode: 'none' },
                { type: 'slider', yAxisIndex: 0, filterMode: 'none' },
                { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
                { type: 'inside', yAxisIndex: 0, filterMode: 'none' }
            ],
            xAxis: { type: 'value', name: viz.xAxisProperty || 'X', splitLine: { show: false } },
            yAxis: { type: 'value', name: viz.yAxisProperty || 'Y', splitLine: { show: false } },
            series: [{
                name: 'Agents',
                type: 'scatter',
                symbolSize: 12,
                itemStyle: { color: '#6200ea' },
                data: []
            }]
        };
    } else if (viz.type === 'heatmap') {
        // We will start with empty category axes.
        // As data comes in, we dynamically compute the bounds (minX, maxX, minY, maxY)
        // and rebuild the axes arrays to exactly fit the data grid.
        viz.heatmapMinX = Infinity;
        viz.heatmapMaxX = -Infinity;
        viz.heatmapMinY = Infinity;
        viz.heatmapMaxY = -Infinity;

        option = {
            tooltip: {
                position: 'top',
                formatter: function (params) {
                    const xLabel = viz.lastHeatmapCategoriesX ? viz.lastHeatmapCategoriesX[params.value[0]] : params.value[0];
                    const yLabel = viz.lastHeatmapCategoriesY ? viz.lastHeatmapCategoriesY[params.value[1]] : params.value[1];
                    const z = Number(params.value[2]).toFixed(2);
                    return `Cell: (${xLabel}, ${yLabel})<br/>${viz.zAxisProperty || 'Value'}: ${z}`;
                }
            },
            toolbox: { feature: { dataZoom: {}, restore: {}, saveAsImage: {} } },
            dataZoom: [
                { type: 'slider', xAxisIndex: 0, filterMode: 'none' },
                { type: 'slider', yAxisIndex: 0, filterMode: 'none' },
                { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
                { type: 'inside', yAxisIndex: 0, filterMode: 'none' }
            ],
            grid: { left: '10%', right: '15%', bottom: '15%', containLabel: true },
            xAxis: { 
                type: 'category', 
                data: [], 
                name: viz.xAxisProperty || 'X', 
                splitArea: { show: true } 
            },
            yAxis: { 
                type: 'category', 
                data: [], 
                name: viz.yAxisProperty || 'Y', 
                splitArea: { show: true } 
            },
            visualMap: {
                min: 0,
                max: 100,
                calculable: true,
                orient: 'vertical',
                right: '2%',
                top: 'center',
                inRange: {
                    color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8',
                        '#ffffbf', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
                }
            },
            series: [{
                name: viz.title || 'Heatmap',
                type: 'heatmap',
                data: [],
                label: { show: false },
                emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } }
            }]
        };
    } else if (viz.type === 'line') {
        option = {
            tooltip: { trigger: 'axis' },
            toolbox: { feature: { dataZoom: {}, restore: {} } },
            xAxis: { type: 'category', data: Array.from({length: 50}, (_, i) => i) },
            yAxis: { type: 'value', name: viz.xAxisProperty || 'Metric' },
            series: []
        };
    } else if (viz.type === 'bar') {
        option = {
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            xAxis: { type: 'category', data: [] },
            yAxis: { type: 'value', name: 'Count' },
            series: [{
                type: 'bar',
                data: [],
                itemStyle: { color: '#03a9f4' }
            }]
        };
    } else if (viz.type === 'graph') {
        option = {
            tooltip: { formatter: '{b}' },
            toolbox: { feature: { restore: {}, saveAsImage: {} } },
            series: [{
                type: 'graph',
                layout: 'force',
                roam: true,
                label: { show: true, position: 'right' },
                force: { repulsion: 150, edgeLength: 80 },
                data: [],
                links: []
            }]
        };
        viz.agentLinksMap = new Map();
    }

    myChart.setOption(option);
}

export function handleIncomingMetric(agentName, metricData) {
    if (!metricData) return;

    visualizationsConfig.forEach(viz => {
        // Only update this visualization if the agent name starts with the configured category prefix.
        if (viz.targetCategory && !agentName.startsWith(viz.targetCategory))
            return;

        if (viz.type === 'scatter') {
            const xVal = metricData[viz.xAxisProperty];
            const yVal = metricData[viz.yAxisProperty];

            if (xVal !== undefined && yVal !== undefined) {
                let itemStyle = undefined;
                if (viz.colorRule && viz.colorRule.property) {
                    const stateVal = metricData[viz.colorRule.property];
                    if (stateVal && viz.colorRule.map[stateVal]) {
                        itemStyle = { color: viz.colorRule.map[stateVal] };
                    }
                }
                const dataPoint = { name: agentName, value: [xVal, yVal, agentName], itemStyle: itemStyle };
                viz.agentDataMap.set(agentName, dataPoint);
                if (viz.chartInstance) {
                    viz.chartInstance.setOption({ series: [{ data: Array.from(viz.agentDataMap.values()) }] });
                }
            }
        } else if (viz.type === 'heatmap') {
            const xVal = metricData[viz.xAxisProperty];
            const yVal = metricData[viz.yAxisProperty];
            const zVal = metricData[viz.zAxisProperty];

            if (xVal !== undefined && yVal !== undefined && zVal !== undefined) {
                const gridX = Math.round(Number(xVal));
                const gridY = Math.round(Number(yVal));
                const gridZ = Number(zVal);

                const dataPoint = { 
                    name: agentName, 
                    value: [gridX, gridY, gridZ] 
                };
                
                viz.agentDataMap.set(agentName, dataPoint);

                if (viz.chartInstance) {
                    // Calculate dynamic bounds based on all currently known agents
                    let minX = Infinity;
                    let maxX = -Infinity;
                    let minY = Infinity;
                    let maxY = -Infinity;

                    viz.agentDataMap.forEach(d => {
                        minX = Math.min(minX, d.value[0]);
                        maxX = Math.max(maxX, d.value[0]);
                        minY = Math.min(minY, d.value[1]);
                        maxY = Math.max(maxY, d.value[1]);
                    });

                    // Add a small padding (1 unit) around the data bounds
                    minX = minX === Infinity ? 0 : minX - 1;
                    maxX = maxX === -Infinity ? 10 : maxX + 1;
                    minY = minY === Infinity ? 0 : minY - 1;
                    maxY = maxY === -Infinity ? 10 : maxY + 1;

                    // Rebuild the category arrays to exactly match the min-max range
                    const categoriesX = [];
                    for (let i = minX; i <= maxX; i++) { categoriesX.push(String(i)); }

                    const categoriesY = [];
                    for (let i = minY; i <= maxY; i++) { categoriesY.push(String(i)); }

                    // Save categories to viz object so tooltip formatter can use them
                    viz.lastHeatmapCategoriesX = categoriesX;
                    viz.lastHeatmapCategoriesY = categoriesY;

                    // Map agent absolute coordinates to array indices for ECharts
                    const dataWithIndices = Array.from(viz.agentDataMap.values()).map(d => [
                        d.value[0] - minX, // Index on X axis
                        d.value[1] - minY, // Index on Y axis
                        d.value[2]         // Z Value
                    ]);

                    viz.chartInstance.setOption({
                        xAxis: { data: categoriesX },
                        yAxis: { data: categoriesY },
                        series: [{ data: dataWithIndices }]
                    }, { replaceMerge: ['xAxis', 'yAxis'] });
                }
            }
        } else if (viz.type === 'graph') {
            const targets = metricData[viz.xAxisProperty];
            if (targets !== undefined) {
                let itemStyle = undefined;
                if (viz.colorRule && viz.colorRule.property) {
                    const stateVal = metricData[viz.colorRule.property];
                    if (stateVal && viz.colorRule.map[stateVal]) {
                        itemStyle = { color: viz.colorRule.map[stateVal] };
                    }
                }
                if (!viz.agentDataMap.has(agentName)) {
                    viz.agentDataMap.set(agentName, { name: agentName, symbolSize: 20, itemStyle: itemStyle || { color: '#03a9f4' } });
                } else {
                    if (itemStyle) viz.agentDataMap.get(agentName).itemStyle = itemStyle;
                }

                let targetArray = [];
                if (Array.isArray(targets)) {
                    targetArray = targets;
                } else if (typeof targets === 'string') {
                    try {
                        targetArray = JSON.parse(targets);
                        if (!Array.isArray(targetArray)) targetArray = [targets];
                    } catch(e) {
                        targetArray = targets.split(',');
                    }
                }

                const agentLinks = targetArray.map(target => {
                    if (!viz.agentDataMap.has(target)) {
                        viz.agentDataMap.set(target, { name: target, symbolSize: 20, itemStyle: { color: '#03a9f4' } });
                    }
                    return { source: agentName, target: target };
                });

                viz.agentLinksMap.set(agentName, agentLinks);
                const allLinks = [];
                viz.agentLinksMap.forEach(links => allLinks.push(...links));

                if (viz.chartInstance) {
                    viz.chartInstance.setOption({
                        series: [{ data: Array.from(viz.agentDataMap.values()), links: allLinks }]
                    });
                }
            }
        } else if (viz.type === 'bar') {
            const val = metricData[viz.xAxisProperty];
            if (val !== undefined) {
                viz.agentDataMap.set(agentName, val);

                const counts = {};
                viz.agentDataMap.forEach(v => { counts[v] = (counts[v] || 0) + 1; });
                viz.chartInstance.setOption({
                    xAxis: { data: Object.keys(counts) },
                    series: [{ data: Object.values(counts) }]
                });
            }
        } else if (viz.type === 'line') {
            const val = metricData[viz.xAxisProperty];
            if (val !== undefined) {
                if (!viz.agentDataMap.has(agentName)) {
                    viz.agentDataMap.set(agentName, []);
                }
                const history = viz.agentDataMap.get(agentName);

                let numVal = Number(val);
                if (isNaN(numVal)) numVal = 0;

                history.push(numVal);
                if (history.length > 50) history.shift();

                if (viz.chartInstance) {
                    const series = [];
                    viz.agentDataMap.forEach((hist, agent) => {
                        series.push({ name: agent, type: 'line', smooth: true, data: hist });
                    });
                    viz.chartInstance.setOption({ series: series }, { replaceMerge: ['series'] });
                }
            }
        }
    });
}

export function setupVizModal() {
    const modal = document.getElementById('viz-modal');
    const btnOpen = document.getElementById('btn-open-viz-modal');
    const spanClose = document.getElementsByClassName('viz-modal-close')[0];
    const btnSave = document.getElementById('btn-save-viz');

    if (!modal || !btnOpen || !spanClose || !btnSave) return;

    btnOpen.onclick = (e) => {
        e.preventDefault();
        modal.style.display = 'block';
    };

    spanClose.onclick = () => {
        modal.style.display = 'none';
    };

    window.onclick = (event) => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    };

    btnSave.onclick = () => {
        const title = document.getElementById('viz-title').value;
        const type = document.getElementById('viz-type').value;
        const category = document.getElementById('viz-category').value;
        const xaxis = document.getElementById('viz-xaxis').value;
        const yaxis = document.getElementById('viz-yaxis').value;
        const zaxis = document.getElementById('viz-zaxis') ? document.getElementById('viz-zaxis').value : '';
        const widthCls = document.getElementById('viz-width').value;
        const heightCls = document.getElementById('viz-height').value;
        const colorRuleStr = document.getElementById('viz-color-rule').value;

        if (!title || !category) {
            alert("Title and Target Category are required!");
            return;
        }

        let colorRule = null;
        if (colorRuleStr) {
            try {
                colorRule = JSON.parse(colorRuleStr);
            } catch(e) {
                alert("Invalid JSON for Color Rule!");
                return;
            }
        }

        const newVizConfig = {
            id: 'viz_' + Date.now(),
            title: title,
            type: type,
            targetCategory: category,
            xAxisProperty: xaxis,
            yAxisProperty: yaxis,
            zAxisProperty: zaxis,
            widthClass: widthCls,
            heightClass: heightCls,
            colorRule: colorRule,
            chartInstance: null,
            agentDataMap: new Map()
        };

        visualizationsConfig.push(newVizConfig);
        saveConfig();
        createVisualizationWidget(newVizConfig);

        let metricName = xaxis;
        if (type === 'scatter' || type === 'heatmap') {
            metricName = `${xaxis},${yaxis}`;
            if (type === 'heatmap' && zaxis) {
                metricName += `,${zaxis}`;
            }
        }

        sendGlobalCommandWithPayload("deployment", {
            command: "SUBSCRIBE_METRICS",
            category: category,
            metric: metricName
        });

        console.log("Subscribed and created widget dynamically:", newVizConfig);

        modal.style.display = 'none';

        document.getElementById('viz-title').value = '';
        document.getElementById('viz-category').value = '';
        document.getElementById('viz-xaxis').value = '';
        document.getElementById('viz-yaxis').value = '';
        if (document.getElementById('viz-zaxis')) document.getElementById('viz-zaxis').value = '';
        if (document.getElementById('viz-color-rule')) document.getElementById('viz-color-rule').value = '';
    };
}

export function removeAgentFromVisualizations(agentName) {
    visualizationsConfig.forEach(viz => {
        let changed = false;

        if (viz.agentDataMap && viz.agentDataMap.has(agentName)) {
            viz.agentDataMap.delete(agentName);
            changed = true;
        }

        if (viz.agentLinksMap) {
            if (viz.agentLinksMap.has(agentName)) {
                viz.agentLinksMap.delete(agentName);
                changed = true;
            }
            viz.agentLinksMap.forEach((links, source) => {
                const initialLength = links.length;
                const newLinks = links.filter(l => l.target !== agentName);
                if (newLinks.length !== initialLength) {
                    viz.agentLinksMap.set(source, newLinks);
                    changed = true;
                }
            });
        }

        if (changed && viz.chartInstance) {
            if (viz.type === 'scatter') {
                viz.chartInstance.setOption({ series: [{ data: Array.from(viz.agentDataMap.values()) }] });
            } else if (viz.type === 'heatmap') {
                let minX = Infinity;
                let maxX = -Infinity;
                let minY = Infinity;
                let maxY = -Infinity;

                viz.agentDataMap.forEach(d => {
                    minX = Math.min(minX, d.value[0]);
                    maxX = Math.max(maxX, d.value[0]);
                    minY = Math.min(minY, d.value[1]);
                    maxY = Math.max(maxY, d.value[1]);
                });

                minX = minX === Infinity ? 0 : minX - 1;
                maxX = maxX === -Infinity ? 10 : maxX + 1;
                minY = minY === Infinity ? 0 : minY - 1;
                maxY = maxY === -Infinity ? 10 : maxY + 1;

                const categoriesX = [];
                for (let i = minX; i <= maxX; i++) { categoriesX.push(String(i)); }

                const categoriesY = [];
                for (let i = minY; i <= maxY; i++) { categoriesY.push(String(i)); }

                const dataWithIndices = Array.from(viz.agentDataMap.values()).map(d => [
                    d.value[0] - minX,
                    d.value[1] - minY,
                    d.value[2]
                ]);

                viz.chartInstance.setOption({
                    xAxis: { data: categoriesX },
                    yAxis: { data: categoriesY },
                    series: [{ data: dataWithIndices }]
                }, { replaceMerge: ['xAxis', 'yAxis'] });
            } else if (viz.type === 'graph') {
                const allLinks = [];
                viz.agentLinksMap.forEach(links => allLinks.push(...links));
                viz.chartInstance.setOption({
                    series: [{ data: Array.from(viz.agentDataMap.values()), links: allLinks }]
                });
            } else if (viz.type === 'bar') {
                const counts = {};
                viz.agentDataMap.forEach(v => { counts[v] = (counts[v] || 0) + 1; });
                viz.chartInstance.setOption({
                    xAxis: { data: Object.keys(counts) },
                    series: [{ data: Object.values(counts) }]
                });
            } else if (viz.type === 'line') {
                const series = [];
                viz.agentDataMap.forEach((hist, agent) => {
                    series.push({ name: agent, type: 'line', smooth: true, data: hist });
                });
                viz.chartInstance.setOption({ series: series }, { replaceMerge: ['series'] });
            }
        }
    });
}