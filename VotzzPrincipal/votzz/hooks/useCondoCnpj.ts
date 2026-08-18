import { useState } from 'react';

export interface CondoData {
  razaoSocial: string;
  cnpj: string;
  cep: string;
  logradouro: string;
  numero: string;
  bairro: string;
  cidade: string;
  uf: string;
}

export function useCondoCnpj() {
  const [loadingCnpj, setLoadingCnpj] = useState(false);
  const [errorCnpj, setErrorCnpj] = useState<string | null>(null);

  // Remove caracteres não numéricos
  const cleanCnpj = (cnpj: string) => cnpj.replace(/\D/g, '');

  const fetchCondoData = async (cnpjInput: string): Promise<CondoData | null> => {
    const cnpj = cleanCnpj(cnpjInput);

    // Validação básica de tamanho
    if (cnpj.length !== 14) {
      if (cnpj.length > 0) setErrorCnpj("CNPJ deve conter 14 dígitos.");
      return null;
    }

    setLoadingCnpj(true);
    setErrorCnpj(null);

    try {
      // --- TENTATIVA 1: OpenCNPJ ---
      // Nota: Definimos um timeout de 3s. Se a OpenCNPJ demorar, abortamos e vamos para a BrasilAPI.
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3000);

      try {
        const res = await fetch(`https://opencnpj.org/v1/cnpj/${cnpj}`, { 
          signal: controller.signal 
        });
        clearTimeout(timeoutId);

        if (!res.ok) throw new Error('Falha na OpenCNPJ');

        const data = await res.json();
        
        setLoadingCnpj(false);
        return {
          razaoSocial: data.nome || '',
          cnpj: cnpjInput,
          // OpenCNPJ geralmente retorna CEP formatado, removemos a formatação
          cep: data.cep ? data.cep.replace(/\D/g, '') : '', 
          logradouro: data.logradouro || '',
          numero: data.numero || '',
          bairro: data.bairro || '',
          cidade: data.municipio || '',
          uf: data.uf || ''
        };

      } catch (err) {
        console.warn("OpenCNPJ falhou ou demorou, tentando BrasilAPI...", err);
        // Não lançamos erro aqui para permitir o fallback
      }

      // --- TENTATIVA 2: BrasilAPI (Fallback) ---
      const resBrasil = await fetch(`https://brasilapi.com.br/api/cnpj/v1/${cnpj}`);
      
      if (!resBrasil.ok) {
        if (resBrasil.status === 404) throw new Error("CNPJ não encontrado.");
        throw new Error("Erro ao consultar CNPJ.");
      }

      const dataBrasil = await resBrasil.json();

      setLoadingCnpj(false);
      return {
        razaoSocial: dataBrasil.razao_social,
        cnpj: cnpjInput,
        cep: dataBrasil.cep,
        logradouro: dataBrasil.logradouro,
        numero: dataBrasil.numero,
        bairro: dataBrasil.bairro,
        cidade: dataBrasil.municipio,
        uf: dataBrasil.uf
      };

    } catch (err: any) {
      setLoadingCnpj(false);
      setErrorCnpj(err.message || "Não foi possível buscar os dados.");
      return null;
    }
  };

  return { fetchCondoData, loadingCnpj, errorCnpj };
}