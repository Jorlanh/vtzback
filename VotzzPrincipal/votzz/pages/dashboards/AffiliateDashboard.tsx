import React, { useEffect, useState } from 'react';
import { api } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { Wallet, Link as LinkIcon, TrendingUp, Lock, Save, Edit2, X, ShieldCheck, Eye, EyeOff } from 'lucide-react';

const maskData = (data: string | undefined, type: 'email' | 'phone' | 'pix' | 'cpf') => {
  if (!data) return 'Não cadastrado';
  if (type === 'phone') return data; 
  if (type === 'email') {
    const parts = data.split('@');
    if (parts.length < 2) return data;
    return `${parts[0].substring(0, 3)}****@${parts[1]}`;
  }
  if (type === 'cpf') return `***.***.${data.slice(-5)}`;
  if (type === 'pix') {
      if (data.includes('@')) return maskData(data, 'email');
      if (data.length === 11 && !isNaN(Number(data))) return maskData(data, 'phone');
      return `${data.substring(0, 4)}****${data.slice(-4)}`;
  }
  return data;
};

// VALIDAÇÃO MODERNA (Mín 8, sem padrões óbvios)
const validatePassword = (pass: string) => {
    if (pass.length < 8) return { valid: false, msg: "A senha deve ter no mínimo 8 caracteres." };
    const weakPasswords = ["12345678", "password", "qwertyui", "votzz123", "admin123"];
    if (weakPasswords.includes(pass.toLowerCase())) return { valid: false, msg: "Senha muito fraca. Evite sequências óbvias." };
    return { valid: true };
};

export default function AffiliateDashboard() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('STATS'); 

  return (
    <div className="p-8 bg-slate-50 min-h-screen">
      <h1 className="text-3xl font-bold text-slate-800 mb-2">Painel do Parceiro Votzz</h1>
      <p className="text-slate-500 mb-8">Acompanhe seus ganhos e gerencie sua conta.</p>
      
      <div className="flex gap-4 mb-8 border-b border-slate-200">
        <button onClick={() => setActiveTab('STATS')} className={`pb-3 px-4 font-medium transition-colors ${activeTab === 'STATS' ? 'border-b-2 border-emerald-500 text-emerald-600' : 'text-slate-500 hover:text-slate-700'}`}>
           Visão Geral
        </button>
        <button onClick={() => setActiveTab('PROFILE')} className={`pb-3 px-4 font-medium transition-colors ${activeTab === 'PROFILE' ? 'border-b-2 border-emerald-500 text-emerald-600' : 'text-slate-500 hover:text-slate-700'}`}>
           Meus Dados
        </button>
      </div>

      {activeTab === 'STATS' && <AffiliateStats user={user} />}
      {activeTab === 'PROFILE' && <AffiliateProfile user={user} />}
    </div>
  );
}

function AffiliateStats({ user }: { user: any }) {
  const [data, setData] = useState<any>(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      if (!user) return;
      try {
        const response = await api.get(`/afiliado/dashboard`);
        setData(response.data);
      } catch (error) {
        console.error('Erro ao carregar dashboard', error);
      }
    };
    fetchDashboard();
  }, [user]);

  if (!data) return <div className="text-slate-500">Carregando seus dados...</div>;

  return (
    <div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        <div className="bg-emerald-600 text-white p-6 rounded-lg shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <span>Saldo Disponível (Saque)</span>
            <Wallet />
          </div>
          <span className="text-4xl font-bold">R$ {data.saldoDisponivel?.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
        </div>
        
        <div className="bg-white p-6 rounded-lg shadow-lg border border-slate-200">
          <div className="flex items-center justify-between mb-4 text-slate-500">
            <span>Saldo Futuro (A Liberar)</span>
            <TrendingUp />
          </div>
          <span className="text-4xl font-bold text-slate-800">R$ {data.saldoFuturo?.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</span>
        </div>
      </div>

      <div className="bg-white p-6 rounded-lg shadow border border-slate-100">
        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2 text-slate-700">
          <LinkIcon className="w-5 h-5 text-emerald-500" /> Seu Link de Indicação
        </h3>
        <div className="flex gap-4">
          <input 
            readOnly 
            value={data.linkIndicacao || `https://votzz.com/register?ref=${user?.id}`}
            className="flex-1 p-3 bg-slate-50 rounded border border-slate-300 text-slate-600 font-mono text-sm"
          />
          <button 
            onClick={() => {
              navigator.clipboard.writeText(data.linkIndicacao);
              alert("Link copiado!");
            }}
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-6 py-2 rounded font-medium transition-colors"
          >
            Copiar
          </button>
        </div>
      </div>
    </div>
  );
}

function AffiliateProfile({ user }: { user: any }) {
  const [editingField, setEditingField] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');
  const [confirmPass, setConfirmPass] = useState('');
  const [showPass, setShowPass] = useState(false);
  
  const handleSave = async (field: string) => {
    try {
        if (field === 'password') {
            if (editValue !== confirmPass) {
                alert("ERRO: As senhas não coincidem.");
                return;
            }
            const check = validatePassword(editValue);
            if (!check.valid) {
                alert(`ERRO: ${check.msg}`);
                return;
            }
        }

        await api.patch('/afiliado/profile', { [field]: editValue });
        alert('Dados atualizados com sucesso!');
        setEditingField(null);
        setEditValue('');
        setConfirmPass('');
        window.location.reload(); 
    } catch (error: any) {
        alert(error.response?.data?.message || error.response?.data || 'Erro ao atualizar.');
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 max-w-3xl overflow-hidden">
      <div className="p-6 border-b border-slate-100 bg-slate-50 flex justify-between items-center">
        <div>
            <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                <Lock className="w-5 h-5 text-emerald-500" />
                Dados Seguros
            </h3>
            <p className="text-sm text-slate-500">Informações protegidas. Clique em alterar para editar.</p>
        </div>
        <button onClick={() => alert("Em breve: 2FA")} className="bg-slate-800 hover:bg-slate-900 text-white px-4 py-2 rounded-lg text-sm font-bold flex items-center gap-2">
            <ShieldCheck className="w-4 h-4" />
            {user?.is2faEnabled ? "Gerenciar 2FA" : "Ativar 2FA"}
        </button>
      </div>
      
      <div className="p-6 space-y-6">
          <DataItem label="E-mail" value={maskData(user?.email, 'email')} field="email" editingField={editingField} setEditingField={setEditingField} setEditValue={setEditValue} handleSave={handleSave} />
          
          <DataItem label="WhatsApp" value={maskData(user?.whatsapp, 'phone')} field="whatsapp" editingField={editingField} setEditingField={setEditingField} setEditValue={setEditValue} handleSave={handleSave} />
          
          <DataItem label="Chave PIX" value={maskData(user?.chavePix || "Não definida", 'pix')} field="chavePix" editingField={editingField} setEditingField={setEditingField} setEditValue={setEditValue} handleSave={handleSave} />

          <div className="border border-slate-200 rounded-lg p-4 bg-slate-50">
              <div className="flex justify-between items-center">
                  <div>
                      <p className="text-xs font-bold text-slate-400 uppercase">Senha</p>
                      <p className="font-mono text-slate-700 text-lg">••••••••••</p>
                  </div>
                  <button onClick={() => { setEditingField('password'); setEditValue(''); setConfirmPass(''); }} className="text-sm font-bold text-emerald-600 hover:underline">
                      Alterar
                  </button>
              </div>

              {editingField === 'password' && (
                 <div className="mt-4 p-4 bg-white border border-emerald-200 rounded-lg space-y-3 animate-fadeIn">
                    <p className="text-sm text-slate-500 mb-2">Requisitos: Mínimo 8 caracteres. Evite sequências óbvias.</p>
                    
                    <div className="relative">
                        <input 
                            type={showPass ? "text" : "password"} 
                            placeholder="Nova Senha" 
                            onChange={e => setEditValue(e.target.value)} 
                            className="w-full p-2 border rounded pr-10" 
                        />
                        <button type="button" onClick={() => setShowPass(!showPass)} className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600">
                            {showPass ? <EyeOff size={18}/> : <Eye size={18}/>}
                        </button>
                    </div>

                    <div className="relative">
                        <input 
                            type={showPass ? "text" : "password"} 
                            placeholder="Confirmar Nova Senha" 
                            onChange={e => setConfirmPass(e.target.value)} 
                            className={`w-full p-2 border rounded pr-10 ${confirmPass && editValue !== confirmPass ? 'border-red-500' : ''}`} 
                        />
                    </div>
                    
                    {confirmPass && editValue !== confirmPass && (
                        <p className="text-xs text-red-500 font-bold">As senhas não coincidem!</p>
                    )}

                    <div className="flex gap-2 pt-2">
                        <button onClick={() => handleSave('password')} className="bg-emerald-600 text-white px-4 py-2 rounded text-sm font-bold hover:bg-emerald-700 flex items-center gap-1">
                            <Save size={16} /> Salvar Senha
                        </button>
                        <button onClick={() => setEditingField(null)} className="bg-slate-200 text-slate-700 px-4 py-2 rounded text-sm font-bold hover:bg-slate-300">
                            Cancelar
                        </button>
                    </div>
                 </div>
              )}
          </div>
      </div>
    </div>
  );
}

const DataItem = ({ label, value, field, editingField, setEditingField, setEditValue, handleSave }: any) => {
    const isEditing = editingField === field;
    return (
        <div className="border border-slate-200 rounded-lg p-4 bg-slate-50 hover:border-emerald-200 transition-colors">
            <div className="flex justify-between items-center">
                <div>
                    <p className="text-xs font-bold text-slate-400 uppercase">{label}</p>
                    <p className="font-mono text-slate-700 text-lg">{value}</p>
                </div>
                {!isEditing && (
                    <button onClick={() => { setEditingField(field); setEditValue(''); }} className="text-sm font-bold text-emerald-600 hover:underline flex items-center gap-1">
                        <Edit2 size={14} /> Alterar
                    </button>
                )}
            </div>
            {isEditing && (
                <div className="mt-3 flex gap-2">
                    <input className="flex-1 p-2 border rounded text-sm" placeholder={`Novo ${label}`} onChange={(e) => setEditValue(e.target.value)} />
                    <button onClick={() => handleSave(field)} className="bg-emerald-600 text-white px-3 py-1.5 rounded text-sm font-bold hover:bg-emerald-700">Salvar</button>
                    <button onClick={() => setEditingField(null)} className="bg-slate-200 text-slate-600 px-3 py-1.5 rounded text-sm font-bold">X</button>
                </div>
            )}
        </div>
    );
};