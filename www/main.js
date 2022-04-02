class BoardTile extends HTMLElement {
    constructor() {
        super()

        let template = document.getElementById('board-tile')
        let templateContent = template.content

        const shadowRoot = this.attachShadow({mode: 'open'})
        shadowRoot.appendChild(templateContent.cloneNode(true))
    }
}

customElements.define('board-tile', BoardTile)

let board = document.getElementById('board')
for (let i = 0; i < 11 * 11; i++) {
    board.appendChild(document.createElement('board-tile'))
}
