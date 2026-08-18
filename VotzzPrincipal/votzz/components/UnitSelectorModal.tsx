import React, { useState, useEffect } from 'react';
import { Layers, X } from 'lucide-react';

interface UnitSelectorModalProps {
  units: string[];
  isOpen: boolean;
  onConfirm: (selected: string[]) => void;
  onCancel: () => void;
}

const UnitSelectorModal: React.FC<UnitSelectorModalProps> = ({ units, isOpen, onConfirm, onCancel }) => {
  const [selected, setSelected] = useState<string[]>(units);

  // Resetar seleção ao abrir para garantir que todas comecem marcadas
  useEffect(() => {
    if (isOpen) setSelected(units);
  }, [isOpen, units]);

  if (!isOpen) return null;

  const toggleUnit = (unit: string) => {
    setSelected(prev => prev.includes(unit) ? prev.filter(u => u !== unit) : [...prev, unit]);
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in">
      <div className="bg-white rounded-[2.5rem] p-8 max-w-sm w-full shadow-2xl border border-slate-100 scale-in-center">
        <div className="bg-emerald-100 w-16 h-16 rounded-full flex items-center justify-center mb-6 mx-auto">
          <Layers className="text-emerald-600 w-8 h-8" />
        </div>
        <h3 className="text-xl font-black text-slate-800 text-center mb-2">Votar por Unidade</h3>
        <p className="text-sm text-slate-500 text-center mb-6">Selecione as unidades para este voto:</p>
        
        <div className="space-y-2 mb-8 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
          {units.map(u => (
            <label key={u} className={`flex items-center gap-3 p-4 rounded-2xl border-2 transition-all cursor-pointer ${selected.includes(u) ? 'border-emerald-500 bg-emerald-50' : 'border-slate-100 hover:bg-slate-50'}`}>
              <input 
                type="checkbox" 
                checked={selected.includes(u)} 
                onChange={() => toggleUnit(u)} 
                className="w-5 h-5 text-emerald-600 rounded-lg border-slate-300 focus:ring-emerald-500" 
              />
              <span className="font-bold text-slate-700">{u}</span>
            </label>
          ))}
        </div>

        <div className="space-y-3">
          <button 
            onClick={() => onConfirm(selected)} 
            disabled={selected.length === 0} 
            className="w-full bg-slate-900 text-white font-black py-4 rounded-2xl shadow-xl hover:bg-slate-800 disabled:opacity-30 transition-all"
          >
            Confirmar {selected.length} Unidade(s)
          </button>
          <button onClick={onCancel} className="w-full text-slate-400 font-bold py-2 hover:text-slate-600 transition-colors text-center">Cancelar</button>
        </div>
      </div>
    </div>
  );
};

export default UnitSelectorModal;