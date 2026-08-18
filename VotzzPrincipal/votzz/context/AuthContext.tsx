import React, { createContext, useState, useEffect, useContext } from 'react';
import api from '../services/api';
import { LoginResponse, User } from '../types';
import { useNavigate } from 'react-router-dom';

interface AuthContextData {
  user: User | null;
  isAuthenticated: boolean;
  login: (data: LoginResponse) => void;
  selectContext: (userId: string) => Promise<void>;
  signOut: () => void;
  loading: boolean;
  updateUser: (data: Partial<User>) => void;
}

const AuthContext = createContext<AuthContextData>({} as AuthContextData);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const storedUser = localStorage.getItem('@Votzz:user');
    const storedToken = localStorage.getItem('@Votzz:token');

    if (storedUser && storedToken) {
      try {
        const parsedUser = JSON.parse(storedUser);
        
        // Sincroniza os headers padrão do axios com os dados do localStorage (Boot da aplicação)
        api.defaults.headers.authorization = `Bearer ${storedToken}`;
        
        const tid = parsedUser.tenantId || parsedUser.tenant?.id;
        if (tid && !['temp', 'null', 'undefined'].includes(String(tid))) {
           api.defaults.headers.common['X-Tenant-ID'] = String(tid);
        }

        setUser(parsedUser);
      } catch (e) {
        console.error("Falha ao restaurar sessão", e);
        signOut();
      }
    }
    setLoading(false);
  }, []);

  const processLoginData = (data: LoginResponse) => {
      // Extração segura do ID do Tenant
      const tenantId = data.tenantId || (data as any).tenant?.id || null;
      
      const tenantData = (data as any).tenant || null;
      const tenantName = (data as any).tenantName || (tenantData ? tenantData.nome : null);

      const userToSave: User = {
        id: data.id, 
        nome: data.nome || '',
        name: data.nome || '',
        email: data.email || '',
        role: data.role as any,
        tenantId: tenantId,
        tenant: tenantData ? { ...tenantData, nome: tenantName } : (tenantName ? { id: tenantId!, nome: tenantName } : undefined),
        // @ts-ignore
        tenantName: tenantName,
        bloco: data.bloco,
        unidade: data.unidade,
        cpf: data.cpf,
        whatsapp: (data as any).whatsapp || ''
      };

      // 1. Atualiza Estado
      setUser(userToSave);
      
      // 2. Persiste
      localStorage.setItem('@Votzz:user', JSON.stringify(userToSave));
      localStorage.setItem('@Votzz:token', data.token || '');
      
      // 3. Atualiza Headers da API imediatamente
      api.defaults.headers.authorization = `Bearer ${data.token}`;
      if (tenantId && !['temp', 'null', 'undefined'].includes(String(tenantId))) {
        api.defaults.headers.common['X-Tenant-ID'] = String(tenantId);
      }

      // 4. Redirecionamento
      if (data.role === 'ADMIN') navigate('/admin/dashboard');
      else if (data.role === 'AFILIADO') navigate('/affiliate/dashboard');
      else navigate('/dashboard');
  };

  const login = (data: LoginResponse) => {
      processLoginData(data);
  };

  const selectContext = async (userId: string) => {
      try {
          const response = await api.post('/auth/select-context', { userId });
          processLoginData(response.data);
      } catch (error) {
          console.error("Erro ao selecionar contexto", error);
          alert("Erro ao entrar no perfil selecionado.");
          navigate('/login');
      }
  };

  const signOut = () => {
    localStorage.removeItem('@Votzz:user');
    localStorage.removeItem('@Votzz:token');
    setUser(null);
    delete api.defaults.headers.authorization;
    delete api.defaults.headers.common['X-Tenant-ID'];
    navigate('/login');
  };

  const updateUser = (data: Partial<User>) => {
      if (user) {
          const updatedUser = { ...user, ...data };
          setUser(updatedUser);
          localStorage.setItem('@Votzz:user', JSON.stringify(updatedUser));
          
          // Se o tenantId mudou no update, atualiza o header
          const tid = updatedUser.tenantId || updatedUser.tenant?.id;
          if (tid) api.defaults.headers.common['X-Tenant-ID'] = String(tid);
      }
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, selectContext, signOut, loading, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);