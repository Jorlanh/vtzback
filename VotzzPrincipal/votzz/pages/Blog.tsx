import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { 
  Search, 
  Calendar, 
  User, 
  Tag, 
  ArrowRight, 
  ChevronRight, 
  Mail, 
  Facebook, 
  Twitter, 
  Linkedin,
  Instagram,
  Menu,
  X
} from 'lucide-react';
import { Logo } from '../components/Logo';
// Removed external import to fix "Cannot find module" error
// import { MockService } from '../services/mockDataService'; 
import { BlogPost } from '../types';

// Extended interface locally to handle missing props in types.ts
interface ExtendedBlogPost extends BlogPost {
    imageUrl?: string;
    tags?: string[];
    image?: string; // Fallback
}

// --- INLINED MOCK SERVICE ---
const MockService = {
  getBlogPosts: async (): Promise<ExtendedBlogPost[]> => {
    return [
      {
        id: '1',
        title: 'Como realizar uma Assembleia Digital com segurança jurídica',
        slug: 'assembleia-digital-seguranca-juridica',
        content: '<p>A realização de assembleias virtuais tornou-se uma realidade...</p>',
        date: new Date().toISOString(),
        author: 'Dra. Ana Silva',
        category: 'Legislação',
        excerpt: 'Entenda os requisitos legais para validar suas decisões online.',
        imageUrl: 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        image: 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        tags: ['Jurídico', 'Assembleia', 'Tecnologia']
      },
      {
        id: '2',
        title: '5 Dicas para reduzir a inadimplência no seu condomínio',
        slug: 'reduzir-inadimplencia-condominio',
        content: '<p>A inadimplência é um dos maiores desafios...</p>',
        date: new Date(Date.now() - 86400000).toISOString(),
        author: 'Carlos Eduardo',
        category: 'Finanças',
        excerpt: 'Estratégias práticas para melhorar o fluxo de caixa.',
        imageUrl: 'https://images.unsplash.com/photo-1554224155-98406858d0cb?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        image: 'https://images.unsplash.com/photo-1554224155-98406858d0cb?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        tags: ['Finanças', 'Gestão', 'Dicas']
      },
      {
        id: '3',
        title: 'Tecnologia e Segurança: O futuro da portaria remota',
        slug: 'futuro-portaria-remota',
        content: '<p>Sistemas de acesso modernos garantem mais segurança...</p>',
        date: new Date(Date.now() - 172800000).toISOString(),
        author: 'Eng. Roberto Campos',
        category: 'Tecnologia',
        excerpt: 'Como a automação está transformando a segurança patrimonial.',
        imageUrl: 'https://images.unsplash.com/photo-1558008258-3256797b43f3?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        image: 'https://images.unsplash.com/photo-1558008258-3256797b43f3?ixlib=rb-1.2.1&auto=format&fit=crop&w=1600&q=80',
        tags: ['Tecnologia', 'Segurança', 'Inovação']
      }
    ] as unknown as ExtendedBlogPost[];
  },

  getBlogPostBySlug: async (slug: string): Promise<ExtendedBlogPost | undefined> => {
    const posts = await MockService.getBlogPosts();
    return posts.find(p => p.slug === slug);
  }
};
// ----------------------------

const BlogHeader: React.FC = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${isScrolled ? 'bg-white shadow-sm py-3' : 'bg-slate-900 text-white py-4'}`}>
      <div className="max-w-7xl mx-auto px-4 flex justify-between items-center">
        <Link to="/" className="hover:opacity-80 transition-opacity">
          <Logo theme={isScrolled ? "dark" : "light"} />
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center space-x-8">
          <Link to="/blog" className={`text-sm font-bold ${isScrolled ? 'text-emerald-600' : 'text-white'}`}>Blog</Link>
          <a href="#" className={`text-sm font-medium ${isScrolled ? 'text-slate-600 hover:text-emerald-600' : 'text-slate-300 hover:text-white'}`}>Sobre</a>
        </nav>

        <div className="hidden md:flex items-center gap-4">
          <Link to="/login" className={`text-sm font-medium ${isScrolled ? 'text-emerald-600' : 'text-emerald-400'}`}>Entrar</Link>
          <Link to="/login" state={{ isRegister: true }} className="bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2 rounded-full text-sm font-bold transition-all shadow-md">
            Começar Agora
          </Link>
        </div>

        <button className="md:hidden p-2" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
          {mobileMenuOpen ? <X className={isScrolled ? 'text-slate-800' : 'text-white'} /> : <Menu className={isScrolled ? 'text-slate-800' : 'text-white'} />}
        </button>
      </div>

       {/* Mobile Menu */}
       {mobileMenuOpen && (
          <div className="md:hidden absolute top-full left-0 right-0 bg-white border-t border-slate-100 shadow-lg p-4 flex flex-col space-y-4 animate-in slide-in-from-top-2 text-slate-800">
            <Link to="/blog" className="py-2 font-bold text-emerald-600 border-b border-slate-50">Blog</Link>
            <Link to="/login" className="py-2 text-emerald-600">Entrar</Link>
          </div>
        )}
    </header>
  );
};

const BlogFooter: React.FC = () => (
  <footer className="bg-slate-900 text-slate-400 py-12 mt-20">
    <div className="max-w-7xl mx-auto px-4 grid md:grid-cols-4 gap-8">
      <div>
        <div className="mb-4">
            <Link to="/">
                <Logo theme="light" />
            </Link>
        </div>
        <p className="text-sm">Tecnologia para decisões democráticas.</p>
        <div className="flex gap-4 mt-6">
          <Facebook className="w-5 h-5 hover:text-white cursor-pointer" />
          <Twitter className="w-5 h-5 hover:text-white cursor-pointer" />
          <Instagram className="w-5 h-5 hover:text-white cursor-pointer" />
          <Linkedin className="w-5 h-5 hover:text-white cursor-pointer" />
        </div>
      </div>
      <div>
        <h4 className="text-white font-bold mb-4">Links Rápidos</h4>
        <ul className="space-y-2 text-sm">
          <li><Link to="/blog" className="hover:text-white">Blog</Link></li>
          <li><Link to="/faq" className="hover:text-white">FAQ</Link></li>
          <li><Link to="/testimonials" className="hover:text-white">Depoimentos</Link></li>
          <li><a href="#" className="hover:text-white">Contato</a></li>
        </ul>
      </div>
      <div>
        <h4 className="text-white font-bold mb-4">Contato</h4>
        <ul className="space-y-2 text-sm">
          <li>suporte@votzz.com.br</li>
          <li>Florianópolis - SC</li>
        </ul>
      </div>
      <div>
        <h4 className="text-white font-bold mb-4">Newsletter</h4>
        <p className="text-xs mb-2">Receba novidades do mercado condominial.</p>
        <div className="flex">
          <input type="email" placeholder="Seu e-mail" className="bg-slate-800 border-none rounded-l-lg px-3 py-2 text-sm w-full focus:ring-1 focus:ring-emerald-500" />
          <button className="bg-emerald-600 text-white px-3 py-2 rounded-r-lg hover:bg-emerald-700"><ArrowRight className="w-4 h-4" /></button>
        </div>
      </div>
    </div>
    <div className="max-w-7xl mx-auto px-4 mt-12 pt-8 border-t border-slate-800 text-center text-xs">
      &copy; {new Date().getFullYear()} Votzz. Todos os direitos reservados.
    </div>
  </footer>
);

const Sidebar: React.FC = () => (
  <div className="space-y-8">
    {/* Search */}
    <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
      <h3 className="font-bold text-slate-900 mb-4">Buscar</h3>
      <div className="relative">
        <input type="text" placeholder="Pesquisar artigos..." className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none" />
        <Search className="absolute left-3 top-2.5 h-5 w-5 text-slate-400" />
      </div>
    </div>

    {/* Categories */}
    <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
      <h3 className="font-bold text-slate-900 mb-4">Categorias</h3>
      <ul className="space-y-2">
        {['Gestão', 'Legislação', 'Tecnologia', 'Finanças', 'Convivência'].map(cat => (
          <li key={cat}>
            <a href="#" className="flex items-center justify-between text-slate-600 hover:text-emerald-600 text-sm group">
              <span>{cat}</span>
              <span className="bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full text-xs group-hover:bg-emerald-100 group-hover:text-emerald-700 transition-colors">3</span>
            </a>
          </li>
        ))}
      </ul>
    </div>

    {/* Newsletter Widget */}
    <div className="bg-emerald-50 p-6 rounded-xl border border-emerald-100 text-center">
      <Mail className="w-8 h-8 text-emerald-600 mx-auto mb-3" />
      <h3 className="font-bold text-emerald-900 mb-2">Boletim Semanal</h3>
      <p className="text-sm text-emerald-700 mb-4">Dicas exclusivas para síndicos e gestores.</p>
      <input type="email" placeholder="Seu melhor e-mail" className="w-full mb-2 px-3 py-2 border border-emerald-200 rounded-lg text-sm" />
      <button className="w-full bg-emerald-600 text-white py-2 rounded-lg font-bold text-sm hover:bg-emerald-700 transition-colors">Cadastrar</button>
    </div>
  </div>
);

const BlogList: React.FC<{ posts: ExtendedBlogPost[] }> = ({ posts }) => {
  const featured = posts[0];
  const list = posts.slice(1);

  return (
    <>
      {/* Featured Article */}
      {featured && (
        <section className="mb-16">
          <div className="bg-white rounded-2xl overflow-hidden shadow-sm border border-slate-200 grid md:grid-cols-2 group hover:shadow-lg transition-all duration-300">
             <div className="h-64 md:h-auto overflow-hidden relative">
               <img src={featured.imageUrl || featured.image} alt={featured.title} className="w-full h-full object-cover transform group-hover:scale-105 transition-transform duration-700" />
               <div className="absolute top-4 left-4 bg-emerald-600 text-white text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wide">Destaque</div>
             </div>
             <div className="p-8 flex flex-col justify-center">
               <div className="flex items-center text-xs text-slate-500 mb-3 space-x-3">
                  <span className="flex items-center"><Calendar className="w-3 h-3 mr-1" /> {new Date(featured.date).toLocaleDateString()}</span>
                  <span className="text-emerald-600 font-medium">{featured.category}</span>
               </div>
               <h2 className="text-2xl md:text-3xl font-bold text-slate-900 mb-4 hover:text-emerald-600 transition-colors">
                 <Link to={`/blog/${featured.slug}`}>{featured.title}</Link>
               </h2>
               <p className="text-slate-600 mb-6 line-clamp-3">{featured.excerpt}</p>
               <Link to={`/blog/${featured.slug}`} className="inline-flex items-center text-emerald-600 font-bold hover:text-emerald-700 group/btn">
                 Ler artigo completo <ArrowRight className="ml-2 w-4 h-4 transform group-hover/btn:translate-x-1 transition-transform" />
               </Link>
             </div>
          </div>
        </section>
      )}

      {/* List */}
      <div className="grid md:grid-cols-2 gap-8">
        {list.map(post => (
          <article key={post.id} className="bg-white rounded-xl overflow-hidden border border-slate-200 shadow-sm hover:shadow-md transition-shadow flex flex-col">
            <div className="h-48 overflow-hidden relative">
              <img src={post.imageUrl || post.image} alt={post.title} className="w-full h-full object-cover hover:scale-105 transition-transform duration-500" />
              <span className="absolute bottom-3 left-3 bg-white/90 backdrop-blur text-slate-800 text-xs font-bold px-2 py-1 rounded shadow-sm">
                {post.category}
              </span>
            </div>
            <div className="p-6 flex-1 flex flex-col">
              <div className="flex items-center text-xs text-slate-400 mb-2">
                <Calendar className="w-3 h-3 mr-1" /> {new Date(post.date).toLocaleDateString()}
              </div>
              <h3 className="text-xl font-bold text-slate-900 mb-3 line-clamp-2">
                <Link to={`/blog/${post.slug}`} className="hover:text-emerald-600 transition-colors">{post.title}</Link>
              </h3>
              <p className="text-sm text-slate-600 mb-4 line-clamp-3 flex-1">{post.excerpt}</p>
              <Link to={`/blog/${post.slug}`} className="text-emerald-600 text-sm font-bold flex items-center mt-auto hover:underline">
                Ler artigo <ChevronRight className="w-4 h-4" />
              </Link>
            </div>
          </article>
        ))}
      </div>
    </>
  );
};

const SinglePost: React.FC<{ post: ExtendedBlogPost }> = ({ post }) => {
  return (
    <article className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
      <div className="h-64 md:h-96 w-full relative">
         <img src={post.imageUrl || post.image} alt={post.title} className="w-full h-full object-cover" />
         <div className="absolute inset-0 bg-gradient-to-t from-slate-900/80 to-transparent"></div>
         <div className="absolute bottom-0 left-0 right-0 p-8 text-white">
            <div className="flex items-center gap-4 text-sm mb-3">
              <span className="bg-emerald-600 px-3 py-1 rounded-full font-bold">{post.category}</span>
              <span className="flex items-center"><Calendar className="w-4 h-4 mr-1" /> {new Date(post.date).toLocaleDateString()}</span>
              <span className="flex items-center"><User className="w-4 h-4 mr-1" /> {post.author}</span>
            </div>
            <h1 className="text-3xl md:text-4xl font-bold leading-tight">{post.title}</h1>
         </div>
      </div>
      
      <div className="p-8 md:p-12">
         <div className="prose prose-lg prose-emerald max-w-none text-slate-700" dangerouslySetInnerHTML={{ __html: post.content }} />
         
         <div className="mt-12 pt-8 border-t border-slate-100 flex items-center justify-between">
           <div className="flex items-center gap-2">
             <Tag className="w-4 h-4 text-slate-400" />
             <div className="flex gap-2">
               {post.tags?.map((tag: string) => (
                 <span key={tag} className="text-xs bg-slate-100 text-slate-600 px-2 py-1 rounded hover:bg-slate-200 cursor-pointer">#{tag}</span>
               ))}
             </div>
           </div>
           
           <div className="flex gap-3">
              <button className="p-2 rounded-full bg-blue-600 text-white hover:bg-blue-700"><Facebook className="w-4 h-4" /></button>
              <button className="p-2 rounded-full bg-sky-500 text-white hover:bg-sky-600"><Twitter className="w-4 h-4" /></button>
              <button className="p-2 rounded-full bg-blue-700 text-white hover:bg-blue-800"><Linkedin className="w-4 h-4" /></button>
           </div>
         </div>

         <div className="mt-12 bg-slate-50 p-8 rounded-xl text-center">
            <h3 className="text-xl font-bold text-slate-900 mb-2">Gostou deste artigo?</h3>
            <p className="text-slate-600 mb-6">Explore mais conteúdos ou volte para a página inicial do blog.</p>
            <Link to="/blog" className="inline-block bg-white border border-slate-300 text-slate-700 px-6 py-3 rounded-full font-bold hover:bg-emerald-50 hover:text-emerald-700 hover:border-emerald-200 transition-all">
               Ver todos os artigos
            </Link>
         </div>
      </div>
    </article>
  );
};

const Blog: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const [posts, setPosts] = useState<ExtendedBlogPost[]>([]);
  const [currentPost, setCurrentPost] = useState<ExtendedBlogPost | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      const allPosts = await MockService.getBlogPosts();
      setPosts(allPosts);
      
      if (slug) {
        const post = await MockService.getBlogPostBySlug(slug);
        setCurrentPost(post || null);
      } else {
        setCurrentPost(null);
      }
      setLoading(false);
    };
    load();
  }, [slug]);

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      <BlogHeader />
      
      {/* Hero Header (Only on list view) */}
      {!slug && (
        <div className="bg-slate-900 text-white pt-40 pb-20 px-4 text-center relative overflow-hidden">
          <div className="absolute inset-0 bg-emerald-600/10"></div>
           <h1 className="text-4xl md:text-6xl font-black mb-4 relative z-10">Blog Votzz</h1>
           <p className="text-xl text-slate-300 max-w-2xl mx-auto relative z-10">
             Notícias, atualizações e dicas para uma gestão condominial inteligente e participativa.
           </p>
        </div>
      )}

      {/* Spacing for Single Post Header */}
      {slug && <div className="h-24 bg-slate-900"></div>}

      <main className="max-w-7xl mx-auto px-4 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
          {/* Main Content */}
          <div className="lg:col-span-2">
            {loading ? (
              <div className="animate-pulse space-y-8">
                 <div className="h-64 bg-slate-200 rounded-xl"></div>
                 <div className="h-8 bg-slate-200 rounded w-1/2"></div>
                 <div className="h-4 bg-slate-200 rounded w-full"></div>
              </div>
            ) : slug ? (
              currentPost ? <SinglePost post={currentPost} /> : <div className="text-center py-20 text-slate-500">Artigo não encontrado.</div>
            ) : (
              <BlogList posts={posts} />
            )}
          </div>

          {/* Sidebar */}
          <aside className="hidden lg:block">
             <Sidebar />
          </aside>
        </div>
      </main>

      <BlogFooter />
    </div>
  );
};

export default Blog;