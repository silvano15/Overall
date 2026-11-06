export interface Candidato {
  id: number;
  overall: number;
}

export class SorteioAlgoritmo {
  private static readonly TENTATIVAS_BALANCEADO = 60;

  static quantidadeDeTimes(jogadores: number, tamanhoTime: number): number {
    return Math.floor((jogadores + tamanhoTime - 1) / tamanhoTime);
  }

  static sortear(
    candidatos: Candidato[],
    tamanhoTime: number,
    balancear: boolean
  ): number[][] {
    if (tamanhoTime < 1) throw new Error('Tamanho do time deve ser ao menos 1.');
    const n = candidatos.length;
    const numTimes = this.quantidadeDeTimes(n, tamanhoTime);
    if (numTimes < 2) throw new Error('A lista precisa formar ao menos 2 times.');

    const capacidade = new Array<number>(numTimes).fill(tamanhoTime);
    if (n % tamanhoTime !== 0) {
      capacidade[numTimes - 1] = n % tamanhoTime;
    }

    const media = candidatos.reduce((acc, c) => acc + c.overall, 0) / (n || 1);

    let melhor: Candidato[][] | null = null;
    let melhorPontuacao = Number.MAX_VALUE;
    const tentativas = balancear ? this.TENTATIVAS_BALANCEADO : 1;

    for (let t = 0; t < tentativas; t++) {
      const times = this.montarAleatorio(candidatos, capacidade);
      if (balancear) this.melhorar(times, media);
      const pontuacao = balancear ? this.pontuar(times, media) : 0;
      if (melhor === null || pontuacao < melhorPontuacao) {
        melhor = times;
        melhorPontuacao = pontuacao;
      }
    }

    return (melhor || []).map((time) => time.map((c) => c.id));
  }

  private static montarAleatorio(candidatos: Candidato[], capacidade: number[]): Candidato[][] {
    const numTimes = capacidade.length;
    const demais = [...candidatos];
    for (let i = demais.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [demais[i], demais[j]] = [demais[j], demais[i]];
    }

    const times: Candidato[][] = Array.from({ length: numTimes }, () => []);
    let proximo = 0;
    for (let t = 0; t < numTimes; t++) {
      while (times[t].length < capacidade[t]) {
        times[t].push(demais[proximo++]);
      }
    }
    return times;
  }

  private static melhorar(times: Candidato[][], media: number): void {
    let atual = this.pontuar(times, media);
    let melhorou = true;
    while (melhorou) {
      melhorou = false;
      for (let i = 0; i < times.length; i++) {
        for (let j = i + 1; j < times.length; j++) {
          const ti = times[i];
          const tj = times[j];
          for (let a = 0; a < ti.length; a++) {
            for (let b = 0; b < tj.length; b++) {
              const ca = ti[a];
              const cb = tj[b];
              ti[a] = cb;
              tj[b] = ca;
              const nova = this.pontuar(times, media);
              if (nova < atual - 1e-9) {
                atual = nova;
                melhorou = true;
              } else {
                ti[a] = ca;
                tj[b] = cb;
              }
            }
          }
        }
      }
    }
  }

  private static pontuar(times: Candidato[][], media: number): number {
    let soma = 0;
    for (const time of times) {
      if (time.length === 0) continue;
      const mediaTime = time.reduce((acc, c) => acc + c.overall, 0) / time.length;
      const diff = mediaTime - media;
      soma += diff * diff;
    }
    return soma;
  }
}
