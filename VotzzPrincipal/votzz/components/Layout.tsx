import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Vote, 
  CalendarDays, 
  FileText, 
  LogOut, 
  Menu, 
  X, 
  Settings,
  ShieldAlert,
  MessageSquare,
  Package, // Ícone de Encomendas
  UserCheck // Ícone de Convidados
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Logo } from './Logo';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { user, signOut } = useAuth(); 
  const location = useLocation();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // ID do Super Admin (Deve ser igual ao do Backend)
  const SUPER_ADMIN_ID = "10000000-0000-0000-0000-000000000000";
  
  // Verifica se existe um token de admin salvo (Modo Espião)
  const superAdminToken = localStorage.getItem('@Votzz:superAdminToken');

  const handleLogout = () => {
    if (signOut) signOut();
    navigate('/'); 
  };

  // Função para Voltar ao God Mode (Painel de Admin)
  const handleBackToGodMode = () => {
      if (superAdminToken) {
          localStorage.setItem('@Votzz:token', superAdminToken);
          localStorage.removeItem('@Votzz:superAdminToken');
          window.location.href = '/admin/dashboard';
      }
  };

  const isActive = (path: string) => location.pathname === path;

  // --- LÓGICA DE EXIBIÇÃO DO USUÁRIO ---
  const userName = user?.nome || user?.name || user?.email?.split('@')[0] || 'Usuário';
  
  const getUserSubtitle = () => {
    if (!user) return 'Visitante';

    // 1. Admins
    if (user.role === 'ADMIN') {
        return user.id === SUPER_ADMIN_ID ? 'SUPER ADMIN' : 'Admin Votzz';
    }

    // 2. Outros Cargos
    if (user.role === 'AFILIADO') return 'Parceiro Oficial';
    if (user.role === 'SINDICO') return 'Síndico Profissional';
    if (user.role === 'ADM_CONDO' || user.role === 'MANAGER') return 'Administrador';
    
    // 3. Morador (CORREÇÃO DO BUG "B A")
    const bloco = user.bloco || '';
    const unidade = user.unidade || '';

    // Se bloco for "A" e unidade "202", mostrará "A - Und 202"
    if (bloco && unidade) return `${bloco} - Und ${unidade}`;
    if (unidade && !bloco) return `Und ${unidade}`;
    if (user.role === 'MORADOR') return 'Morador';
    
    return 'Visitante';
  };

  const userSubtitle = getUserSubtitle();
  
  const subtitleColorClass = user?.id === SUPER_ADMIN_ID 
      ? "text-[10px] font-black text-red-500 uppercase tracking-widest" 
      : user?.role === 'ADMIN' 
          ? "text-xs font-bold text-emerald-500" 
          : "text-xs text-emerald-400 truncate font-medium";

  const menuItems = [
    { 
      label: 'Dashboard', 
      path: user?.role === 'AFILIADO' ? '/affiliate/dashboard' : (user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'), 
      icon: LayoutDashboard,
      allowed: ['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER', 'AFILIADO', 'ADMIN']
    },
    { 
      label: 'Governança', 
      path: '/governance', 
      icon: Vote,
      allowed: ['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']
    },
    { 
      label: 'Assembleias', 
      path: '/assemblies', 
      icon: FileText, 
      allowed: ['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']
    },
    { 
      label: 'Encomendas', 
      path: '/orders', 
      icon: Package, 
      allowed: ['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO']
    },
    { 
      label: 'Convidados', 
      path: '/guests', 
      icon: UserCheck, 
      allowed: ['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER', 'PORTEIRO']
    },
    { 
      label: 'Espaços & Reservas', 
      path: '/spaces', 
      icon: CalendarDays,
      allowed: ['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']
    },
    { 
      label: 'Chamados & Ajuda', 
      path: '/tickets', 
      icon: MessageSquare,
      allowed: ['MORADOR', 'SINDICO', 'ADM_CONDO', 'MANAGER']
    },
    { 
      label: 'Relatórios & Auditoria', 
      path: '/reports', 
      icon: ShieldAlert,
      allowed: ['SINDICO', 'ADM_CONDO', 'MANAGER']
    }
  ];

  return (
    <div className="min-h-screen bg-slate-50 flex">
      
      {/* --- SIDEBAR DESKTOP --- */}
      <aside className="hidden md:flex flex-col w-64 bg-slate-900 text-slate-300 fixed h-full z-20 shadow-xl border-r border-slate-800">
        <div className="p-6 flex items-center justify-center border-b border-slate-800">
           <Link to={user?.role === 'AFILIADO' ? "/affiliate/dashboard" : (user?.role === 'ADMIN' ? "/admin/dashboard" : "/dashboard")} className="hover:opacity-80 transition-opacity">
              <Logo theme="dark" />
           </Link>
        </div>

        <nav className="flex-1 p-4 space-y-2 overflow-y-auto custom-scrollbar">
          {menuItems.map((item) => {
            if (user && !item.allowed.includes(user.role)) return null;

            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group ${
                  isActive(item.path) 
                    ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/20 font-bold translate-x-1' 
                    : 'hover:bg-slate-800 hover:text-white hover:translate-x-1'
                }`}
              >
                <item.icon className={`w-5 h-5 ${isActive(item.path) ? 'text-white' : 'text-slate-500 group-hover:text-emerald-400'}`} />
                <span className="text-sm">{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* SEÇÃO DO PERFIL NA BASE (FOOTER) */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/90">
          <div className="flex items-center gap-3 mb-4">
            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-lg border shrink-0 shadow-inner ${user?.id === SUPER_ADMIN_ID ? 'bg-red-500/20 text-red-500 border-red-500/50' : 'bg-emerald-900/50 text-emerald-400 border-emerald-500/30'}`}>
              {userName.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-white font-bold text-sm truncate" title={userName}>
                {userName.split(' ')[0]} {userName.split(' ')[1] || ''}
              </p>
              <p className={subtitleColorClass} title={userSubtitle}>
                {userSubtitle}
              </p>
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-2">
            {user?.role !== 'AFILIADO' && (
                <Link 
                to={user?.role === 'ADMIN' ? '/admin/dashboard' : '/profile'} 
                className="flex items-center justify-center gap-2 bg-slate-800 hover:bg-slate-700 text-xs py-2 rounded-lg transition-colors text-slate-300 hover:text-white border border-slate-700"
                >
                <Settings size={14} /> Perfil
                </Link>
            )}
            
            <button 
              onClick={handleLogout}
              className={`flex items-center justify-center gap-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 hover:text-red-300 text-xs py-2 rounded-lg transition-colors border border-red-900/30 ${user?.role === 'AFILIADO' ? 'col-span-2' : ''}`}
            >
              <LogOut size={14} /> Sair
            </button>
          </div>
        </div>
      </aside>

      {/* --- MOBILE HEADER --- */}
      <div className="md:hidden fixed top-0 left-0 right-0 bg-slate-900 text-white z-30 p-4 flex justify-between items-center shadow-md border-b border-slate-800">
        <Logo theme="dark" size="sm" />
        <button 
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="p-2 hover:bg-slate-800 rounded-lg transition-colors text-slate-300"
        >
          {isMobileMenuOpen ? <X /> : <Menu />}
        </button>
      </div>

      {/* --- MOBILE MENU --- */}
      {isMobileMenuOpen && (
        <div className="md:hidden fixed inset-0 bg-slate-900 z-20 pt-20 px-4 animate-in slide-in-from-top-10 duration-200">
          <nav className="space-y-3">
            {menuItems.map(item => {
               if (user && !item.allowed.includes(user.role)) return null;
               return (
                <Link 
                  key={item.path} 
                  to={item.path} 
                  onClick={() => setIsMobileMenuOpen(false)}
                  className={`flex items-center gap-3 p-4 rounded-xl border ${
                    isActive(item.path) 
                      ? 'bg-emerald-600 text-white border-emerald-500 shadow-md' 
                      : 'text-slate-300 bg-slate-800 border-slate-700'
                  }`}
                >
                  <item.icon size={20} /> <span className="font-bold">{item.label}</span>
                </Link>
               )
            })}
            
            <div className="border-t border-slate-700 pt-6 mt-6">
              <div className="flex items-center gap-3 mb-6 px-2 bg-slate-800/50 p-4 rounded-xl border border-slate-700">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold border ${user?.id === SUPER_ADMIN_ID ? 'bg-red-500/20 text-red-500 border-red-500/50' : 'bg-emerald-900/30 text-emerald-500 border-emerald-500/30'}`}>
                  {userName.charAt(0).toUpperCase()}
                </div>
                <div>
                  <p className="text-white text-sm font-bold">{userName}</p>
                  <p className={subtitleColorClass}>{userSubtitle}</p>
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-3">
                  {user?.role !== 'AFILIADO' && (
                      <Link to={user?.role === 'ADMIN' ? '/admin/dashboard' : '/profile'} onClick={() => setIsMobileMenuOpen(false)} className="flex items-center justify-center gap-2 p-3 rounded-xl text-slate-300 bg-slate-800 border border-slate-700 font-medium">
                        <Settings size={18} /> Dados
                      </Link>
                  )}
                  <button onClick={handleLogout} className={`flex items-center justify-center gap-2 p-3 rounded-xl text-red-400 bg-red-900/20 border border-red-900/50 font-medium ${user?.role === 'AFILIADO' ? 'col-span-2' : ''}`}>
                    <LogOut size={18} /> Sair
                  </button>
              </div>
            </div>
          </nav>
        </div>
      )}

      {/* --- MAIN CONTENT --- */}
      <main className="flex-1 md:ml-64 p-4 md:p-8 mt-16 md:mt-0 overflow-x-hidden w-full">
        
        {/* BARRA DE AVISO DE IMPERSONATION (BOTÃO DE VOLTAR) */}
        {superAdminToken && (
            <div className="bg-red-600 text-white px-6 py-3 rounded-xl mb-6 flex justify-between items-center shadow-lg shadow-red-200 animate-pulse transition-all">
                <div className="flex items-center gap-2 font-bold">
                    <span className="bg-white text-red-600 px-2 py-0.5 rounded text-xs uppercase font-black tracking-wider">Modo Espião</span>
                    Você está acessando como Administrador do Sistema
                </div>
                <button 
                    onClick={handleBackToGodMode}
                    className="bg-white text-red-600 px-4 py-2 rounded-lg font-black text-xs uppercase hover:bg-red-50 transition-colors shadow-sm"
                >
                    Voltar para Votzz Admin
                </button>
            </div>
        )}

        {children}
      </main>
    </div>
  );
};

export default Layout;