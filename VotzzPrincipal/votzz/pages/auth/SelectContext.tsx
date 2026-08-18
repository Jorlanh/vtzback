import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Building, Shield, TrendingUp, ArrowRight, User } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Logo } from '../../components/Logo';

interface ContextOption {
  id: string; // O backend manda 'id' no ProfileOption
  nome: string;
  role: string;
  tenantName: string;
}

export default function SelectContext() {
  const location = useLocation();
  const navigate = useNavigate();
  const { selectContext } = useAuth();
  
  const options: ContextOption[] = location.state?.options || [];

  if (options.length === 0) {
    navigate('/login');
    return null;
  }

  const handleSelect = async (userId: string) => {
    await selectContext(userId);
  };

  const getIcon = (role: string) => {
    if (role === 'ADMIN') return <Shield className="w-6 h-6 text-red-500" />;
    if (role === 'AFILIADO') return <TrendingUp className="w-6 h-6 text-emerald-500" />;
    return <Building className="w-6 h-6 text-blue-500" />;
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center p-4">
      <div className="mb-8">
        <Logo theme="dark" />
      </div>
      
      <div className="max-w-md w-full bg-white rounded-2xl shadow-2xl overflow-hidden">
        <div className="p-8 text-center bg-slate-50 border-b border-slate-100">
          <h2 className="text-2xl font-bold text-slate-800">Onde deseja entrar?</h2>
          <p className="text-slate-500 mt-2 text-sm">Encontramos múltiplos perfis para suas credenciais.</p>
        </div>

        <div className="p-4 space-y-3 max-h-[60vh] overflow-y-auto">
          {options.map((opt) => (
            <button
              key={opt.id}
              onClick={() => handleSelect(opt.id)}
              className="w-full flex items-center justify-between p-4 bg-white border border-slate-200 rounded-xl hover:border-emerald-500 hover:shadow-md transition-all group text-left"
            >
              <div className="flex items-center gap-4">
                <div className="bg-slate-50 p-3 rounded-full group-hover:bg-emerald-50 transition-colors">
                  {getIcon(opt.role)}
                </div>
                <div>
                  <p className="font-bold text-slate-800 text-sm">{opt.tenantName}</p>
                  <div className="flex gap-2 items-center">
                    <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
                        {opt.role === 'ADM_CONDO' ? 'Admin' : opt.role}
                    </span>
                    <span className="text-xs text-slate-500">{opt.nome}</span>
                  </div>
                </div>
              </div>
              <ArrowRight className="text-slate-300 group-hover:text-emerald-500 transition-colors" />
            </button>
          ))}
        </div>

        <div className="p-4 bg-slate-50 text-center border-t border-slate-100">
          <button onClick={() => navigate('/login')} className="text-xs text-slate-500 hover:text-red-500 font-bold">
            Cancelar e voltar
          </button>
        </div>
      </div>
    </div>
  );
}