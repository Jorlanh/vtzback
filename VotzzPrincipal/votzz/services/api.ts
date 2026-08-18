import axios from 'axios';

// Limpa a URL base e define o padrão
const getCleanBaseURL = () => {
  const envUrl = (import.meta as any).env.VITE_API_URL;
  if (envUrl && typeof envUrl === 'string' && envUrl.includes('http')) {
    return envUrl.split(',')[0].trim();
  }
  return 'http://localhost:8080/api';
};

export const api = axios.create({
  baseURL: getCleanBaseURL(),
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('@Votzz:token');
  const storedUser = localStorage.getItem('@Votzz:user');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  if (storedUser) {
    try {
      const user = JSON.parse(storedUser);
      const tenantId = user.tenantId || (user.tenant && user.tenant.id);
      
      if (tenantId && !['temp', 'null', 'undefined', ''].includes(String(tenantId))) {
        config.headers['X-Tenant-ID'] = String(tenantId);
      } else {
        delete config.headers['X-Tenant-ID'];
      }
    } catch (e) {
      console.error("Erro ao parsear usuário para injeção de Tenant", e);
    }
  }

  return config;
}, (error) => {
  return Promise.reject(error);
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    
    // --- TRAVA DE SEGURANÇA PARA A IA (CORRIGIDO) ---
    // Alterado de '/chat/support' para '/ai/' para bater com o seu Controller
    if (error.config?.url?.includes('/ai/')) {
        console.error("Alerta: Falha na comunicação com a Inteligência Artificial S.I.R.I.U.S.");
        return Promise.reject(error);
    }
    
    // --- LÓGICA DE BLOQUEIO FINANCEIRO ---
    if (error.response?.status === 402) {
      const storedUser = localStorage.getItem('@Votzz:user');
      
      if (storedUser) {
        try {
          const user = JSON.parse(storedUser);
          
          if (user.role === 'ADMIN' || user.role === 'SINDICO') {
             window.location.href = '#/pricing'; 
          } else {
             alert("O acesso ao condomínio está temporariamente suspenso. Contate a administração.");
          }
        } catch (e) {
          console.error("Erro ao processar redirecionamento de bloqueio", e);
        }
      }
      return Promise.reject(error);
    }

    // Redireciona para login se o token for inválido (401) OU proibido (403)
    // A trava da IA acima impede que erros de IA caiam aqui e desloguem o usuário
    if ((error.response?.status === 401 || error.response?.status === 403) && !window.location.hash.includes('auth')) {
      console.warn("Sessão expirada ou credenciais alteradas. Redirecionando para login...");
      localStorage.removeItem('@Votzz:token');
      localStorage.removeItem('@Votzz:user');
      window.location.href = '#/auth/login';
      window.location.reload();
    }
    return Promise.reject(error);
  }
);

export default api;