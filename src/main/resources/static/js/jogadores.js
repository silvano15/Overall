async function listarJogadores() {
    try {
        const jogadores = await req('GET', '/jogadores');
        renderizarTabelaJogadores(jogadores);
    } catch (erro) {
        console.error("Erro ao carregar a lista de jogadores:", erro);
        alert("Não foi possível carregar o elenco. Verifique o console.");
    }
}

function renderizarTabelaJogadores(jogadores) {
    const tbody = document.getElementById('tabela-jogadores-body');
    
    if (!tbody) return; 

    tbody.innerHTML = '';

    jogadores.forEach(jogador => {
        const tr = document.createElement('tr');
        
        tr.innerHTML = `
            <td>${jogador.id || '-'}</td>
            <td><strong>${jogador.nome}</strong></td>
            <td>${jogador.posicao}</td>
            <td>
                <button class="btn-editar" onclick="editarJogador(${jogador.id})">✏️ Editar</button>
                <button class="btn-excluir" onclick="excluirJogador(${jogador.id})">🗑️ Excluir</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function salvarJogador(event) {
    if (event) event.preventDefault(); 

    const inputNome = document.getElementById('input-nome-jogador');
    const inputPosicao = document.getElementById('input-posicao-jogador');

    const dadosFormulario = {
        nome: inputNome.value,
        posicao: inputPosicao.value
    };

    try {
        await req('POST', '/jogadores', dadosFormulario);
        alert("Jogador cadastrado com sucesso!");
        
        inputNome.value = '';
        inputPosicao.value = '';

        listarJogadores(); 
    } catch (erro) {
        console.error("Erro ao salvar o jogador:", erro);
        alert("Erro ao cadastrar o jogador.");
    }
}

async function excluirJogador(id) {
    if(confirm("Tem certeza que deseja excluir este jogador? Esta ação não pode ser desfeita.")) {
        try {
            await req('DELETE', `/jogadores/${id}`);
            alert("Jogador excluído com sucesso!");
            listarJogadores();
        } catch (erro) {
            console.error("Erro ao excluir:", erro);
            alert("Erro ao excluir o jogador.");
        }
    }
}

function editarJogador(id) {
    alert(`A função para editar o jogador de ID ${id} será implementada na próxima etapa!`);
}