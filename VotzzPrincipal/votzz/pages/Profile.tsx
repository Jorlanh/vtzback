import React, { useState, useEffect } from 'react';
import { User, Mail, Phone, Lock, Save, Loader2, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
// IMPORTANTE: Importe o componente que criamos
import { TwoFactorSetup } from '../components/TwoFactorSetup';

const Profile: React.FC = () => {
  const { user } = useAuth(); // Pega dados do contexto
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  
  const [formData, setFormData] = useState({
    nome: '',
    email: '',
    whatsapp: '',
    password: '',
    confirmPassword: ''
  });

  // Preenche o form quando o usuário carregar
  useEffect(() => {
    if (user) {
        setFormData(prev => ({
            ...prev,
            nome: user.nome || user.name || '',
            email: user.email || '',
            whatsapp: user.whatsapp || ''
        }));
    }
  }, [user]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (formData.password && formData.password !== formData.confirmPassword) {
      return setErrorMsg("As senhas não conferem.");
    }

    if (!user?.id) {
        return setErrorMsg("Erro de sessão: ID do usuário não encontrado. Faça login novamente.");
    }

    setLoading(true);
    try {
      const payload: any = { 
        nome: formData.nome, 
        email: formData.email, 
        whatsapp: formData.whatsapp 
      };
      
      if (formData.password) {
          payload.password = formData.password;
      }

      await api.patch(`/users/${user.id}`, payload); 
      
      setSuccessMsg("Dados atualizados com sucesso! As alterações aparecerão no próximo login.");
      setFormData(prev => ({ ...prev, password: '', confirmPassword: '' })); // Limpa senha
      
    } catch (error: any) {
      console.error("Erro no update:", error);
      const backendMessage = error.response?.data?.message || error.response?.data;
      setErrorMsg(typeof backendMessage === 'string' ? backendMessage : "Erro ao atualizar dados. Tente novamente.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-in fade-in duration-500 pb-20">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">Meu Perfil</h1>
        <p className="text-slate-500">Gerencie suas informações pessoais e de acesso.</p>
      </div>

      {errorMsg && (
        <div className="bg-red-50 text-red-700 p-4 rounded-xl border border-red-200 flex items-center gap-2">
            <AlertCircle size={20} /> {errorMsg}
        </div>
      )}

      {successMsg && (
        <div className="bg-emerald-50 text-emerald-700 p-4 rounded-xl border border-emerald-200 flex items-center gap-2">
            <User size={20} /> {successMsg}
        </div>
      )}

      {/* FORMULÁRIO DE DADOS PESSOAIS */}
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-xl shadow-sm border border-slate-100 space-y-6">
        
        <div className="grid md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Nome Completo</label>
            <div className="relative">
              <User className="absolute left-3 top-3 h-5 w-5 text-slate-400" />
              <input 
                name="nome"
                type="text" 
                value={formData.nome} 
                onChange={handleChange}
                className="w-full pl-10 p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none" 
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">E-mail</label>
            <div className="relative">
              <Mail className="absolute left-3 top-3 h-5 w-5 text-slate-400" />
              <input 
                name="email"
                type="email" 
                value={formData.email} 
                onChange={handleChange}
                className="w-full pl-10 p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none" 
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">WhatsApp / Telefone</label>
            <div className="relative">
              <Phone className="absolute left-3 top-3 h-5 w-5 text-slate-400" />
              <input 
                name="whatsapp"
                type="text" 
                value={formData.whatsapp} 
                onChange={handleChange}
                className="w-full pl-10 p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none" 
              />
            </div>
          </div>
        </div>

        <div className="pt-6 border-t border-slate-100">
          <h3 className="font-bold text-slate-800 mb-4">Alterar Senha</h3>
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Nova Senha</label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 h-5 w-5 text-slate-400" />
                <input 
                  name="password"
                  type="password" 
                  value={formData.password} 
                  onChange={handleChange}
                  placeholder="Deixe em branco para manter"
                  className="w-full pl-10 p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none" 
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Confirmar Senha</label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 h-5 w-5 text-slate-400" />
                <input 
                  name="confirmPassword"
                  type="password" 
                  value={formData.confirmPassword} 
                  onChange={handleChange}
                  placeholder="Confirme a nova senha"
                  className="w-full pl-10 p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none" 
                />
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4">
          <button 
            type="submit" 
            disabled={loading}
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-2.5 rounded-lg font-bold shadow-md flex items-center gap-2 transition-all disabled:opacity-70"
          >
            {loading ? <Loader2 className="animate-spin h-5 w-5" /> : <Save className="h-5 w-5" />}
            Salvar Alterações
          </button>
        </div>

      </form>

      {/* --- SEÇÃO NOVA DE 2FA --- */}
      <TwoFactorSetup user={user} />
      
    </div>
  );
};

export default Profile;