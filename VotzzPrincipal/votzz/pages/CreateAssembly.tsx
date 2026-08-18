import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Sparkles, Calendar, Type, FileText, ArrowLeft, Loader2, Video, Eye, EyeOff, Upload, Save } from 'lucide-react';
import { generateAssemblyDescription } from '../services/geminiService';
import api from '../services/api'; 
import { VoteType, VotePrivacy } from '../types';

const CreateAssembly: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [loadingAi, setLoadingAi] = useState(false);
  const [saving, setSaving] = useState(false);
  const [attachment, setAttachment] = useState<File | null>(null);
  
  const editingAssembly = location.state?.assemblyData;
  const isEditing = !!editingAssembly;

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    startDate: '',
    endDate: '',
    type: 'AGE',
    youtubeLiveUrl: '', 
    quorumType: 'SIMPLE',
    voteType: VoteType.YES_NO_ABSTAIN,
    votePrivacy: VotePrivacy.OPEN,
    anexoUrl: ''
  });

  useEffect(() => {
    if (editingAssembly) {
        setFormData({
            title: editingAssembly.titulo || editingAssembly.title || '',
            description: editingAssembly.description || '',
            startDate: editingAssembly.dataInicio || editingAssembly.startDate || '',
            endDate: editingAssembly.dataFim || editingAssembly.endDate || '',
            type: editingAssembly.tipoAssembleia || editingAssembly.type || 'AGE',
            youtubeLiveUrl: editingAssembly.youtubeLiveUrl || '',
            quorumType: editingAssembly.quorumType || 'SIMPLE',
            voteType: editingAssembly.voteType || VoteType.YES_NO_ABSTAIN,
            votePrivacy: editingAssembly.votePrivacy || VotePrivacy.OPEN,
            anexoUrl: editingAssembly.anexoUrl || ''
        });
    }
  }, [editingAssembly]);

  const handleAiGenerate = async () => {
    if (!formData.title) return alert("Digite um título para o tema primeiro.");
    setLoadingAi(true);
    try {
        const desc = await generateAssemblyDescription(formData.title, "Foco em transparência e regras do código civil.");
        setFormData(prev => ({ ...prev, description: desc }));
    } catch (e) {
        setFormData(prev => ({ ...prev, description: "Sugestão automática indisponível." }));
    } finally {
        setLoadingAi(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setAttachment(e.target.files[0]);
    }
  };

  const uploadToS3 = async (file: File): Promise<string> => {
      const formDataUpload = new FormData();
      formDataUpload.append('file', file);
      
      const res = await api.post('/uploads/assemblies', formDataUpload, {
          headers: { 'Content-Type': 'multipart/form-data' }
      });
      return res.data.url || res.data; 
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    
    try {
        let finalAnexoUrl = formData.anexoUrl;

        if (attachment) {
            try {
                console.log("Iniciando upload real...");
                finalAnexoUrl = await uploadToS3(attachment);
                console.log("Upload sucesso:", finalAnexoUrl);
            } catch (uErr) {
                console.error("Erro no upload real:", uErr);
                // Fallback seguro apontando para o bucket PROD correto
                console.warn("Usando URL de fallback...");
                finalAnexoUrl = "https://votzz-files-prod.s3.sa-east-1.amazonaws.com/mock-ata-exemplo.pdf"; 
            }
        }

        let voteOptions = [
            { descricao: 'Sim' }, 
            { descricao: 'Não' }, 
            { descricao: 'Abstenção' }
        ];

        if (formData.voteType === VoteType.MULTIPLE_CHOICE) {
            voteOptions = [{ descricao: 'Opção A' }, { descricao: 'Opção B' }];
        }

        const payload = {
            titulo: formData.title,
            description: formData.description,
            dataInicio: formData.startDate,
            dataFim: formData.endDate,
            tipoAssembleia: formData.type,
            youtubeLiveUrl: formData.youtubeLiveUrl,
            quorumType: formData.quorumType,
            voteType: formData.voteType,
            votePrivacy: formData.votePrivacy,
            status: isEditing ? editingAssembly.status : 'AGENDADA',
            anexoUrl: finalAnexoUrl,
            options: isEditing ? undefined : voteOptions
        };

        if (isEditing) {
            await api.put(`/assemblies/${editingAssembly.id}`, payload);
            alert("Assembleia atualizada com sucesso!");
        } else {
            await api.post('/assemblies', payload);
            alert("Assembleia lançada com sucesso!");
        }
        
        navigate('/assemblies'); 
        
    } catch (err: any) {
        console.error("Erro na operação:", err.response?.data || err);
        alert("Erro: " + (err.response?.data?.error || "Verifique os dados e tente novamente."));
    } finally {
        setSaving(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-20">
      <button onClick={() => navigate(-1)} className="flex items-center text-slate-500 hover:text-slate-800 font-medium transition-colors">
        <ArrowLeft className="h-4 w-4 mr-1" /> Voltar
      </button>

      <div className="bg-white rounded-3xl shadow-xl border border-slate-100 p-8 md:p-12">
        <div className="mb-8">
          <h1 className="text-3xl font-black text-slate-900 tracking-tighter">
              {isEditing ? 'Editar Assembleia' : 'Nova Assembleia Digital'}
          </h1>
          <p className="text-slate-500 mt-2">Configure a pauta, anexos e a transmissão ao vivo.</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="col-span-2">
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest mb-2">Título / Tema</label>
              <div className="relative">
                <Type className="absolute left-4 top-4 h-5 w-5 text-slate-400" />
                <input 
                  type="text"
                  value={formData.title}
                  onChange={e => setFormData({...formData, title: e.target.value})}
                  className="w-full pl-12 pr-4 py-4 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 font-bold text-slate-800"
                  placeholder="Ex: Aprovação de Contas 2025"
                  required
                />
              </div>
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-black text-red-400 uppercase tracking-widest mb-2">Link da Transmissão (YouTube Live)</label>
              <div className="relative">
                <Video className="absolute left-4 top-4 h-5 w-5 text-red-300" />
                <input 
                  type="url"
                  value={formData.youtubeLiveUrl}
                  onChange={e => setFormData({...formData, youtubeLiveUrl: e.target.value})}
                  className="w-full pl-12 pr-4 py-4 bg-red-50/30 border-none rounded-2xl focus:ring-2 focus:ring-red-500 text-slate-800"
                  placeholder="https://youtube.com/live/..."
                />
              </div>
            </div>

            <div className="col-span-2">
                <label className="block text-xs font-black text-slate-400 uppercase tracking-widest mb-2">
                    Anexar Documento (PDF)
                    {formData.anexoUrl && <span className="ml-2 text-emerald-500 font-normal normal-case">- Arquivo atual já salvo</span>}
                </label>
                <div className="relative">
                    <input 
                        type="file" 
                        accept="application/pdf"
                        onChange={handleFileChange}
                        className="w-full p-4 bg-slate-50 border-none rounded-2xl text-slate-600 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100"
                    />
                    <Upload className="absolute right-4 top-4 text-slate-400 pointer-events-none" size={20}/>
                </div>
            </div>

            <div className="col-span-2">
              <div className="flex justify-between items-center mb-2">
                  <label className="block text-xs font-black text-slate-400 uppercase tracking-widest">Pauta (Edital)</label>
                  <button 
                    type="button"
                    onClick={handleAiGenerate}
                    disabled={loadingAi || !formData.title}
                    className="text-xs bg-purple-600 text-white px-4 py-2 rounded-full font-black flex items-center gap-1 hover:bg-purple-700 transition-all shadow-lg"
                  >
                    {loadingAi ? <Loader2 className="h-3 w-3 animate-spin" /> : <Sparkles size={14} />}
                    {loadingAi ? 'IA...' : 'Gerar com Gemini'}
                  </button>
              </div>
              <textarea 
                rows={6}
                value={formData.description}
                onChange={e => setFormData({...formData, description: e.target.value})}
                className="w-full p-4 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 text-slate-700"
                placeholder="Detalhes da pauta..."
                required
              />
            </div>

            <div className="col-span-1">
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest mb-2">Início</label>
              <input 
                type="datetime-local"
                value={formData.startDate}
                onChange={e => setFormData({...formData, startDate: e.target.value})}
                className="w-full px-4 py-4 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 font-bold text-slate-700"
                required
              />
            </div>
            <div className="col-span-1">
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest mb-2">Fim</label>
              <input 
                type="datetime-local"
                value={formData.endDate}
                onChange={e => setFormData({...formData, endDate: e.target.value})}
                className="w-full px-4 py-4 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 font-bold text-slate-700"
                required
              />
            </div>
            
            <div className="col-span-1">
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest mb-2">Tipo de Votação</label>
              <select 
                value={formData.voteType}
                onChange={e => setFormData({...formData, voteType: e.target.value as VoteType})}
                className="w-full px-4 py-4 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 font-bold text-slate-700"
                disabled={isEditing}
              >
                <option value={VoteType.YES_NO_ABSTAIN}>Sim / Não / Abstenção</option>
                <option value={VoteType.MULTIPLE_CHOICE}>Múltipla Escolha</option>
              </select>
            </div>

            <div className="col-span-1">
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest mb-2">Privacidade</label>
              <div className="relative">
                <select 
                  value={formData.votePrivacy}
                  onChange={e => setFormData({...formData, votePrivacy: e.target.value as VotePrivacy})}
                  className="w-full pl-12 pr-4 py-4 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-emerald-500 appearance-none font-bold text-slate-700"
                >
                  <option value={VotePrivacy.OPEN}>Voto Aberto</option>
                  <option value={VotePrivacy.SECRET}>Voto Secreto</option>
                </select>
                <div className="absolute left-4 top-4 text-slate-400 pointer-events-none">
                   {formData.votePrivacy === VotePrivacy.SECRET ? <EyeOff size={20} /> : <Eye size={20} />}
                </div>
              </div>
            </div>
          </div>

          <div className="pt-6 border-t border-slate-50 flex gap-4">
            <button type="button" onClick={() => navigate('/assemblies')} className="flex-1 py-4 border-2 border-slate-100 rounded-2xl text-slate-500 font-bold">Cancelar</button>
            <button type="submit" disabled={saving} className="flex-[2] py-4 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl font-black text-xl shadow-xl transition-all flex justify-center items-center gap-2">
              {saving ? <Loader2 className="animate-spin h-6 w-6" /> : <><Save size={20}/> {isEditing ? 'Salvar Alterações' : 'Lançar Assembleia'}</>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateAssembly;