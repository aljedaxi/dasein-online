import {create, scaleLinear, scaleOrdinal, line as d3Line} from "https://cdn.jsdelivr.net/npm/d3@7/+esm";

const radialScale = scaleLinear().domain([0, 3]).range([0, 250])
const ticks = [1, 2, 3]
const line = d3Line().x(d => d.x).y(d => d.y)

const constructChart = props => {
	const {
		width,
		height,
		marginTop,
		marginRight,
		marginBottom,
		marginLeft,
		features,
		cafeByFeature,
	} = props
	const angleToCoord = (angle, value) => {
		const x = Math.cos(angle) * radialScale(value)
		const y = Math.sin(angle) * radialScale(value)
		return {x: width / 2 + x, y: height / 2 - y}
	}
	const getPathCoordinates = features => datum => {
		const {color} = datum
		const points = features.map(({value}, idx, {length}) => ({
			color,
			...angleToCoord((Math.PI / 2) + (2 * Math.PI * idx / length), datum[value] ?? 0)
		}))
		// loop the path all the way back around
		points.push(points[0])
		console.log({points, features: features.map(f => f.value), datum})
		return points
	}

	const svg = create("svg").attr("width", width).attr("height", height);

	const featureData = features.map(({label, title, dataset}, idx, {length}) => {
		const {href} = dataset
		const angle = (Math.PI / 2) + (2 * Math.PI * idx / length)
		return {label, title, href, angle, lineCoord: angleToCoord(angle, 3), labelCoord: angleToCoord(angle, 3.3)}
	})

	svg.selectAll('path')
		.data(Object.values(cafeByFeature))
		.join(enter =>
			enter.append('path')
			.datum(getPathCoordinates(features))
			.attr('d', line)
			.attr('stroke-width', 3)
			.attr('stroke', ([{color}]) => color)
			.attr('fill', ([{color}]) => color)
			.attr('stroke-opacity', 1)
			.attr('fill-opacity', 0.2))

	svg.selectAll('circle')
		.data(ticks)
		.join(enter =>
			enter.append('circle')
				.attr('cx', width / 2)
				.attr('cy', height / 2)
				.attr('fill', 'none')
				.attr('stroke', 'gray')
				.attr('r', radialScale))

	svg.selectAll('line')
		.data(featureData)
		.join(enter => 
			enter.append('line')
			.attr('x1', width / 2)
			.attr('y1', height / 2)
			.attr('x2', d => d.lineCoord.x)
			.attr('y2', d => d.lineCoord.y)
			.attr('stroke', 'gray'))

	svg.selectAll('.axislabel')
		.data(featureData)
		.join(enter => 
			enter
			.append('a')
			.attr('href', d => d.href)
			.append('text')
			.attr('x', d => d.labelCoord.x)
			.attr('y', d => d.labelCoord.y)
			.attr('text-anchor', 'middle')
			.text(d => d.label))

	return svg
}

class SpiderGraph extends HTMLElement {
	constructor() {
		super()
	}
	connectedCallback() {
		const featureList = document.querySelector(this.getAttribute('features'))

		const cafes = document.querySelector(this.getAttribute('data'))
		const cafeByFeature = {}
		let idx = 0
		for (const option of cafes.options) {
			cafeByFeature[option.value] = option.dataset
		}

		const id = this.getAttribute('id') ?? 'spider'
		const svg = constructChart({
			features: [...featureList.options],
			cafeByFeature,
			width:        this.getAttribute("width") ?? 600,
			height:       this.getAttribute("height") ?? 600,
			marginTop:    this.getAttribute("marginTop") ?? 20,
			marginRight:  this.getAttribute("marginRight") ?? 20,
			marginBottom: this.getAttribute("marginBottom") ?? 20,
			marginLeft:   this.getAttribute("marginLeft") ?? 20,
		})
		const svgNode = svg.node()
		const title = document.createElement('title')
		title.innerHTML = this.getAttribute('label')
		title.id = `${id}-title`

		svgNode.role = 'group'
		svgNode.setAttribute('aria-labelledby', `${id}-title`)
		svgNode.prepend(title)

		const style = document.createElement('style')
		style.textContent = `
			@namespace svg url(http://www.w3.org/2000/svg);

			svg|a:link,
			svg|a:visited {
				cursor: pointer;
			}

			svg|a text,
			text svg|a {
				fill: var(--fg);
				text-decoration: underline;
			}
		`

		this.prepend(style)
		this.prepend(svgNode)
	}
}

class SpiderLegend extends HTMLElement {
	constructor() {
		super()
	}
	connectedCallback() {
		const cafes = document.querySelector(this.getAttribute('datalist'))
		const cafeByFeature = {}
		for (const option of cafes.options) {
			cafeByFeature[option.value] = option.dataset
			cafeByFeature[option.value].label = option.label
		}
		const ul = document.createElement('ul')
		ul.innerHTML = [...Object.values(cafeByFeature)].map(({color, label, href, summary}) =>
			`<li>
				<h2 style="color: ${color}">
					<a style="color: inherit" href="${href}">${label}</a>
				</h2>
				${summary}
			</li>`).join('')
		this.append(ul)
	}
}

customElements.define('spider-graph', SpiderGraph)
customElements.define('spider-legend', SpiderLegend)
