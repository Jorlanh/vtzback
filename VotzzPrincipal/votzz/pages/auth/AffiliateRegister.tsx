import React, { useState } from 'react';
import { api } from '../../services/api';
import { useNavigate, Link } from 'react-router-dom';
import { Logo } from '../../components/Logo';
import { TrendingUp, User, Mail, Lock, Phone, CreditCard, DollarSign, Tag, Eye, EyeOff } from 'lucide-react';

export function AffiliateRegister() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    nome: '', email: '', cpf: '', whatsapp: '', password: '', confirmPassword: '', chavePix: '', codigoAfiliado: ''
  });
  const [loading, setLoading] = useState(false);
  const [showPass, setShowPass] = useState(false);

  // NOVA VALIDAÇÃO DE SENHA (MODERNA)
  const validatePassword = (pass: string) => {
    // 1. Mínimo 8 caracteres
    if (pass.length < 8) return { valid: false, msg: "A senha deve ter no mínimo 8 caracteres." };
    
    // 2. Proibir sequências óbvias e palavras comuns
    const weakPasswords = ["12345678", "87654321", "password", "qwertyui", "votzz123", "admin123"];
    if (weakPasswords.includes(pass.toLowerCase())) return { valid: false, msg: "Essa senha é muito comum. Tente algo mais forte." };

    // 3. Proibir sequências numéricas simples (ex: 11111111)
    if (/^(\d)\1+$/.test(pass)) return { valid: false, msg: "Evite repetir o mesmo caractere." };

    return { valid: true };
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (formData.password !== formData.confirmPassword) {
      alert("ERRO: As senhas não coincidem!");
      return;
    }

    const passCheck = validatePassword(formData.password);
    if (!passCheck.valid) {
      alert(`ERRO: ${passCheck.msg}`);
      return;
    }

    setLoading(true);
    try {
      await api.post('/auth/register-affiliate', formData);
      alert("Cadastro realizado com sucesso! Faça login para acessar seu painel.");
      navigate('/login');
    } catch (error: any) {
      console.error(error);
      const msg = error.response?.data?.message || error.response?.data || "Erro ao realizar cadastro.";
      alert(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center px-4 py-12">
      <div className="mb-8"><Link to="/"><Logo theme="dark" /></Link></div>
      
      <div className="max-w-md w-full bg-slate-800 rounded-2xl p-8 shadow-2xl border border-slate-700">
        <div className="text-center mb-8">
          <div className="w-12 h-12 bg-emerald-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
            <TrendingUp className="text-emerald-500 w-6 h-6" />
          </div>
          <h2 className="text-2xl font-bold text-white">Seja um Parceiro Votzz</h2>
          <p className="text-slate-400 mt-2">Crie sua conta e comece a faturar.</p>
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase mb-1 ml-1">Nome Completo</label>
            <div className="relative">
              <User className="absolute left-3 top-3 text-slate-500 w-5 h-5" />
              <input required name="nome" placeholder="Seu nome" className="w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border border-slate-700 outline-none" onChange={handleChange} />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-emerald-400 uppercase mb-1 ml-1">Crie seu Código de Indicação</label>
            <div className="relative">
              <Tag className="absolute left-3 top-3 text-emerald-500 w-5 h-5" />
              <input required name="codigoAfiliado" placeholder="Ex: JOAOVENDAS" className="w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border border-emerald-500/50 focus:border-emerald-500 outline-none" onChange={handleChange} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
                <label className="block text-xs font-bold text-slate-400 uppercase mb-1 ml-1">CPF</label>
                <div className="relative">
                    <CreditCard className="absolute left-3 top-3 text-slate-500 w-5 h-5" />
                    <input required name="cpf" placeholder="000.000.000-00" className="w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border border-slate-700 outline-none" onChange={handleChange} />
                </div>
            </div>
            <div>
                <label className="block text-xs font-bold text-slate-400 uppercase mb-1 ml-1">WhatsApp</label>
                <div className="relative">
                    <Phone className="absolute left-3 top-3 text-slate-500 w-5 h-5" />
                    <input required name="whatsapp" placeholder="(00) 00000-0000" className="w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border border-slate-700 outline-none" onChange={handleChange} />
                </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase mb-1 ml-1">E-mail</label>
            <div className="relative">
              <Mail className="absolute left-3 top-3 text-slate-500 w-5 h-5" />
              <input type="email" required name="email" placeholder="seu@email.com" className="w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border border-slate-700 outline-none" onChange={handleChange} />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-400 uppercase mb-1 ml-1">Chave PIX</label>
            <div className="relative">
              <DollarSign className="absolute left-3 top-3 text-slate-500 w-5 h-5" />
              <input required name="chavePix" placeholder="Para receber comissões" className="w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border border-slate-700 outline-none" onChange={handleChange} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase mb-1 ml-1">Senha</label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 text-slate-500 w-5 h-5" />
                <input type={showPass ? "text" : "password"} required name="password" className="w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border border-slate-700 outline-none" onChange={handleChange} />
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-400 uppercase mb-1 ml-1">Confirmar</label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 text-slate-500 w-5 h-5" />
                <input type={showPass ? "text" : "password"} required name="confirmPassword" className={`w-full bg-slate-900 text-white rounded-lg pl-10 p-3 border outline-none ${formData.confirmPassword && formData.password !== formData.confirmPassword ? 'border-red-500' : 'border-slate-700'}`} onChange={handleChange} />
                <button type="button" onClick={() => setShowPass(!showPass)} className="absolute right-3 top-3 text-slate-500 hover:text-white">
                    {showPass ? <EyeOff size={16}/> : <Eye size={16}/>}
                </button>
              </div>
            </div>
          </div>
          
          <p className="text-[10px] text-slate-500 leading-tight">
            * Mínimo 8 caracteres. Dica: Use uma frase curta que você lembre.
          </p>

          <button type="submit" disabled={loading} className="w-full bg-emerald-500 hover:bg-emerald-600 text-slate-900 font-bold py-4 rounded-lg transition-all shadow-lg mt-4">
            {loading ? 'Criando conta...' : 'Criar Conta de Parceiro'}
          </button>
        </form>
        <div className="mt-6 text-center">
          <Link to="/login" className="text-slate-400 hover:text-white text-sm">Já tem conta? Fazer Login</Link>
        </div>
      </div>
    </div>
  );
}