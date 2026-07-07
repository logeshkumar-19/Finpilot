import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import './App.css';

import {
  TrendingUp, TrendingDown, DollarSign, Calendar, PieChart as PieIcon,
  Award, MessageSquare, Sparkles, Settings, Mic, FileText, Plus,
  Target, Activity, AlertTriangle, Bell, User, LogOut, Trash2,
  Info, Clock, Compass, ArrowRight, RefreshCw, Zap, HelpCircle, Eye, EyeOff, Brain
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts';
import SettingsDashboard from './SettingsDashboard';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const COLORS = ['#00d4ff', '#8b5cf6', '#10b981', '#f97316', '#ef4444', '#facc15'];

export default function App() {
  // --- Core State Management ---
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [email, setEmail] = useState(localStorage.getItem('email') || '');
  const [name, setName] = useState(localStorage.getItem('name') || 'Guest');
  const [currency, setCurrency] = useState(localStorage.getItem('currency') || 'INR');
  const [monthlyIncome, setMonthlyIncome] = useState(localStorage.getItem('monthlyIncome') || '0');

  // --- UI & UX State ---
  const [activeTab, setActiveTab] = useState('dashboard');
  const [loading, setLoading] = useState(false);
  const [notifyMessage, setNotifyMessage] = useState(null);

  // --- Monthly Income State ---
  const [showIncomeModal, setShowIncomeModal] = useState(false);
  const [showIncomeBanner, setShowIncomeBanner] = useState(false);
  const [modalIncome, setModalIncome] = useState('');
  const [modalSalaryDate, setModalSalaryDate] = useState('5');
  const [modalIncomeType, setModalIncomeType] = useState('');

  // --- Authentication Form State ---
  const [isLogin, setIsLogin] = useState(true);
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authName, setAuthName] = useState('');
  const [authIncome, setAuthIncome] = useState('0');
  const [authError, setAuthError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // --- Data Records State ---
  const [dashboard, setDashboard] = useState(null);
  const [expenses, setExpenses] = useState([]);
  const [goals, setGoals] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);

  // --- Interaction & Simulation Inputs State ---
  const [rawText, setRawText] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [voiceText, setVoiceText] = useState('');
  const [simName, setSimName] = useState('Premium Gaming Laptop');
  const [simCost, setSimCost] = useState('80000');
  const [simResult, setSimResult] = useState(null);
  const [simLoading, setSimLoading] = useState(false);

  const [whatIfTarget, setWhatIfTarget] = useState('Daily Cafe Coffee');
  const [whatIfAmount, setWhatIfAmount] = useState('150');
  const [whatIfFreq, setWhatIfFreq] = useState('5');
  const [whatIfResult, setWhatIfResult] = useState(null);

  const [newGoalName, setNewGoalName] = useState('');
  const [newGoalTarget, setNewGoalTarget] = useState('');
  const [newGoalDate, setNewGoalDate] = useState('');

  // --- Chatbot Coach State ---
  const [chatMessage, setChatMessage] = useState('');
  const [chatLog, setChatLog] = useState([
    { sender: 'ai', text: 'Hello! I am FinPilot, your intelligent financial coach. Ask me anything about your salary leaks, coffee habits, or whether you can afford that new laptop!' }
  ]);
  const [chatLoading, setChatLoading] = useState(false);
  const chatEndRef = useRef(null);

  // --- API Configuration ---
  const api = axios.create({
    baseURL: API_BASE,
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });

  useEffect(() => {
    if (token) fetchData();
  }, [token]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatLog]);

  // --- Notification Toast Handler ---
  const showNotification = (msg, type = 'info') => {
    setNotifyMessage({ text: msg, type });
    setTimeout(() => setNotifyMessage(null), 5000);
  };

  // --- Network Operations ---
  const fetchData = async () => {
    setLoading(true);
    try {
      try {
        const incomeRes = await api.get('/income/current');
        setMonthlyIncome(incomeRes.data.income);
        setShowIncomeModal(false);
      } catch (err) {
        if (err.response && err.response.status === 404) {
          setShowIncomeModal(true);
        }
      }

      const [dashRes, expRes, goalRes, subRes] = await Promise.all([
        api.get('/dashboard/summary'),
        api.get('/expenses'),
        api.get('/goals'),
        api.get('/subscriptions')
      ]);
      setDashboard(dashRes.data);
      setExpenses(expRes.data);
      setGoals(goalRes.data);
      setSubscriptions(subRes.data);
    } catch (err) {
      console.error('Error syncing records:', err);
      showNotification('Session expired or connection failed. Please log in again.', 'error');
      handleLogout();
    } finally {
      setLoading(false);
    }
  };

  const handleIncomeSubmit = async (e) => {
    e.preventDefault();
    if (!modalIncome) return;
    setLoading(true);
    try {
      await api.post('/income', {
        income: parseFloat(modalIncome),
        salaryDate: parseInt(modalSalaryDate),
        incomeType: modalIncomeType
      });
      showNotification('Monthly income saved!', 'success');
      setShowIncomeModal(false);
      setShowIncomeBanner(false);
      fetchData();
    } catch (err) {
      showNotification(err.response?.data?.error || 'Failed to save income.', 'error');
      setLoading(false);
    }
  };

  // Password validation helper
  const checkPasswordConditions = (pass) => {
    return {
      minLength: pass.length >= 8,
      hasUpper: /[A-Z]/.test(pass),
      hasLower: /[a-z]/.test(pass),
      hasDigit: /[0-9]/.test(pass),
      hasSpecial: /[!@#$%^&*()_+\-=\[\]{}|;:',.<>?]/.test(pass)
    };
  };

  const handleAuth = async (e) => {
    e.preventDefault();
    setAuthError('');
    try {
      if (isLogin) {
        const res = await axios.post(`${API_BASE}/auth/login`, { email: authEmail, password: authPassword });
        saveAuth(res.data);
      } else {
        const res = await axios.post(`${API_BASE}/auth/register`, {
          email: authEmail,
          password: authPassword,
          firstName: authName,
          monthlyIncomeBudget: parseFloat(authIncome)
        });
        saveAuth(res.data);
      }
    } catch (err) {
      setAuthError(err.response?.data?.message || err.response?.data || 'Connection to backend failed. Make sure Spring Boot is running.');
    }
  };

  const saveAuth = (data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('email', data.email);
    localStorage.setItem('name', data.firstName || 'User');
    localStorage.setItem('currency', data.currency || 'INR');
    localStorage.setItem('monthlyIncome', data.monthlyIncomeBudget?.toString() || '0');

    setToken(data.token);
    setEmail(data.email);
    setName(data.firstName || 'User');
    setCurrency(data.currency || 'INR');
    setMonthlyIncome(data.monthlyIncomeBudget?.toString() || '0');
    showNotification(`Welcome back, ${data.firstName || 'User'}!`, 'success');
  };

  const handleLogout = () => {
    localStorage.clear();
    setToken('');
    setEmail('');
    setName('Guest');
    setDashboard(null);
    setExpenses([]);
    setGoals([]);
    setSubscriptions([]);
  };

  // --- Expense Engine Handlers ---
  const submitRawExpense = async (e) => {
    e.preventDefault();
    if (!rawText.trim()) return;
    setLoading(true);
    try {
      const res = await api.post('/expenses/parse-text', { text: rawText });
      showNotification(`Expense logged at ${res.data.merchant} for ₹${res.data.amount}!`, 'success');
      setRawText('');
      fetchData();
    } catch (err) {
      showNotification('Failed to parse text natively.', 'warning');
    } finally {
      setLoading(false);
    }
  };

  const handleVoiceRecordSimulate = () => {
    setIsRecording(true);
    setVoiceText('Transcribing audio...');
    const sampleTranscripts = [
      'Spent 350 rupees on pizza at 2 PM',
      'I paid 150 rupees for coffee at Starbucks',
      'Paid 450 rupees for Uber taxi ride',
      'Spent 2500 rupees on clothes shopping at Amazon'
    ];
    const randomIdx = Math.floor(Math.random() * sampleTranscripts.length);

    setTimeout(async () => {
      setIsRecording(false);
      const text = sampleTranscripts[randomIdx];
      setVoiceText(`Parsed: "${text}"`);
      setLoading(true);
      try {
        const formData = new FormData();
        const file = new File([new Blob(['mock data'])], lowerCaseFileMockName(text), { type: 'audio/wav' });
        formData.append('file', file);
        const res = await api.post('/expenses/voice', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        showNotification(`Voice expense logged: ${res.data.merchant} - ₹${res.data.amount}`, 'success');
        fetchData();
      } catch (err) {
        showNotification('Voice entry processing error.', 'error');
      } finally {
        setLoading(false);
      }
    }, 2000);
  };

  const lowerCaseFileMockName = (transcript) => {
    if (transcript.includes('coffee')) return 'starbucks_coffee.wav';
    if (transcript.includes('pizza')) return 'pizza_lunch.wav';
    if (transcript.includes('Uber')) return 'uber_cab.wav';
    return 'shopping_apparel.wav';
  };

  const handleReceiptUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setLoading(true);
    showNotification('Processing receipt with Google Vision OCR...', 'info');
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post('/expenses/receipt', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      showNotification(`OCR Extracted: ₹${res.data.amount} spent at ${res.data.merchant}`, 'success');
      fetchData();
    } catch (err) {
      showNotification('Receipt OCR processing failed.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // --- Sandboxed Projections ---
  const runPurchaseSimulation = async (e) => {
    e.preventDefault();
    if (!simName.trim() || !simCost) return;
    setSimLoading(true);
    try {
      const res = await api.post('/simulator/purchase', { itemName: simName, itemCost: parseFloat(simCost) });
      setSimResult(res.data);
    } catch (err) {
      showNotification('Simulation computation failed.', 'error');
    } finally {
      setSimLoading(false);
    }
  };

  const runWhatIfSimulation = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/simulator/what-if', {
        reductionTarget: whatIfTarget,
        amountPerIncident: parseFloat(whatIfAmount),
        frequencyPerWeek: parseFloat(whatIfFreq)
      });
      setWhatIfResult(res.data);
    } catch (err) {
      showNotification('Sandbox projection computation failed.', 'error');
    }
  };

  // --- Goal & Subscription Tracking ---
  const handleCreateGoal = async (e) => {
    e.preventDefault();
    if (!newGoalName.trim() || !newGoalTarget) return;
    setLoading(true);
    try {
      await api.post('/goals', {
        name: newGoalName,
        targetAmount: parseFloat(newGoalTarget),
        targetDate: newGoalDate || null
      });
      showNotification(`Goal '${newGoalName}' planned successfully!`, 'success');
      setNewGoalName('');
      setNewGoalTarget('');
      setNewGoalDate('');
      fetchData();
    } catch (err) {
      showNotification('Failed to process planned goal.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const contributeGoal = async (goalId, amount) => {
    setLoading(true);
    try {
      await api.post(`/goals/${goalId}/contribute`, { amount });
      showNotification('Savings contribution updated!', 'success');
      fetchData();
    } catch (err) {
      showNotification('Failed to process contribution.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const toggleSub = async (subId) => {
    try {
      const res = await api.patch(`/subscriptions/${subId}/toggle`);
      showNotification(`Subscription ${res.data.merchant} is now ${res.data.isActive ? 'Active' : 'Cancelled'}`, 'info');
      fetchData();
    } catch (err) {
      showNotification('Failed to update subscription status.', 'error');
    }
  };

  const toggleSubUnused = async (subId, isUnused) => {
    try {
      await api.patch(`/subscriptions/${subId}/unused`, { unused: !isUnused });
      showNotification('Subscription leak flags adjusted.', 'info');
      fetchData();
    } catch (err) {
      showNotification('Failed to switch subscription flag.', 'error');
    }
  };

  const submitChat = async (e) => {
    e.preventDefault();
    if (!chatMessage.trim()) return;

    const userMsg = { sender: 'user', text: chatMessage };
    setChatLog(prev => [...prev, userMsg]);
    const input = chatMessage;
    setChatMessage('');
    setChatLoading(true);
    try {
      const res = await api.post('/coach/chat', { message: input });
      setChatLog(prev => [...prev, { sender: 'ai', text: res.data.reply }]);
    } catch (err) {
      setChatLog(prev => [...prev, { sender: 'ai', text: 'Advisory engine unavailable right now. Try again shortly.' }]);
    } finally {
      setChatLoading(false);
    }
  };

  const authPasswordConditions = checkPasswordConditions(authPassword);
  const isAuthPasswordValid = Object.values(authPasswordConditions).every(Boolean);

  // --- Auth View (Rendered when unauthorized) ---
  if (!token) {
    return (
      <div className="auth-container">
        <div className="auth-glow auth-glow-top" />
        <div className="auth-glow auth-glow-bottom" />

        <div className="auth-card">
          <div className="auth-logo">
            <div className="auth-logo-icon">
              <Compass />
            </div>
            <h1 className="auth-title">FinPilot AI</h1>
            <p className="auth-subtitle">Intelligent Financial Decision Simulator</p>
          </div>

          {authError && (
            <div className="auth-error">
              <AlertTriangle />
              <span>{authError}</span>
            </div>
          )}

          <form onSubmit={handleAuth} className="auth-form">
            {!isLogin && (
              <div className="auth-form-group">
                <label className="auth-form-label">First Name</label>
                <input
                  type="text"
                  className="auth-form-input"
                  placeholder="John"
                  value={authName}
                  onChange={(e) => setAuthName(e.target.value)}
                  required
                />
              </div>
            )}
            <div className="auth-form-group">
              <label className="auth-form-label">Email Address</label>
              <input
                type="email"
                className="auth-form-input"
                placeholder="demo@finpilot.ai"
                value={authEmail}
                onChange={(e) => setAuthEmail(e.target.value)}
                required
              />
            </div>
            <div className="auth-form-group">
              <label className="auth-form-label">Password</label>
              <div className="auth-password-wrapper">
                <input
                  type={showPassword ? "text" : "password"}
                  className="auth-form-input"
                  placeholder="••••••••"
                  value={authPassword}
                  onChange={(e) => setAuthPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="auth-password-toggle"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {!isLogin && (
                <div className="password-requirements-box mt-3 p-3 rounded bg-slate-800/40 border border-slate-800 text-xs space-y-1">
                  <p className="text-slate-400 font-semibold mb-2">Password Requirements:</p>
                  <div className="flex items-center gap-2">
                    <span className={authPasswordConditions.minLength ? "text-emerald-400 font-bold" : "text-slate-500"}>
                      {authPasswordConditions.minLength ? "✓" : "○"}
                    </span>
                    <span className={authPasswordConditions.minLength ? "text-slate-300" : "text-slate-400"}>Minimum 8 characters</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={authPasswordConditions.hasUpper ? "text-emerald-400 font-bold" : "text-slate-500"}>
                      {authPasswordConditions.hasUpper ? "✓" : "○"}
                    </span>
                    <span className={authPasswordConditions.hasUpper ? "text-slate-300" : "text-slate-400"}>At least one uppercase letter (A–Z)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={authPasswordConditions.hasLower ? "text-emerald-400 font-bold" : "text-slate-500"}>
                      {authPasswordConditions.hasLower ? "✓" : "○"}
                    </span>
                    <span className={authPasswordConditions.hasLower ? "text-slate-300" : "text-slate-400"}>At least one lowercase letter (a–z)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={authPasswordConditions.hasDigit ? "text-emerald-400 font-bold" : "text-slate-500"}>
                      {authPasswordConditions.hasDigit ? "✓" : "○"}
                    </span>
                    <span className={authPasswordConditions.hasDigit ? "text-slate-300" : "text-slate-400"}>At least one numeric digit (0–9)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={authPasswordConditions.hasSpecial ? "text-emerald-400 font-bold" : "text-slate-500"}>
                      {authPasswordConditions.hasSpecial ? "✓" : "○"}
                    </span>
                    <span className={authPasswordConditions.hasSpecial ? "text-slate-300" : "text-slate-400"}>At least one special character (!@#$%^&*()_+-=[]{}|;:',.&lt;&gt;?)</span>
                  </div>
                </div>
              )}
            </div>
            {!isLogin && (
              <div className="auth-form-group">
                <label className="auth-form-label">Monthly Income Budget (₹)</label>
                <input
                  type="number"
                  className="auth-form-input"
                  placeholder="100000"
                  value={authIncome}
                  onChange={(e) => setAuthIncome(e.target.value)}
                  required
                />
              </div>
            )}
            <button 
              type="submit" 
              className="auth-submit-btn"
              disabled={!isLogin && !isAuthPasswordValid}
              style={(!isLogin && !isAuthPasswordValid) ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
            >
              <span>{isLogin ? 'Sign In' : 'Create Account'}</span>
              <ArrowRight size={16} />
            </button>
          </form>

          <div className="auth-switch">
            <button
              onClick={() => { setIsLogin(!isLogin); setAuthError(''); }}
              className="auth-switch-btn"
            >
              {isLogin ? "Don't have an account? Sign Up" : "Already have an account? Sign In"}
            </button>
            {isLogin && (
              <div className="auth-demo-hint">
                <p>💡 <span className="font-medium text-slate-400">Developer Sandbox:</span></p>
                <p className="mt-0.5">Use email <strong>demo@finpilot.ai</strong> and password <strong>FinPilot@123</strong> to instantly explore the app with pre-loaded data.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  // --- Main Application Interface View ---
  return (
    <div className="app-container">
      {/* Monthly Income Modal */}
      {showIncomeModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-800 p-8 rounded-2xl w-full max-w-md shadow-2xl flex flex-col gap-6 animate-in zoom-in-95 duration-300">
            <div className="text-center space-y-2">
              <div className="mx-auto w-12 h-12 bg-gradient-to-tr from-[#00d4ff] to-[#8b5cf6] rounded-xl flex items-center justify-center mb-4">
                <DollarSign className="text-white" size={24} />
              </div>
              <h2 className="text-2xl font-bold text-white tracking-tight">Welcome!</h2>
              <p className="text-slate-400">Please enter your income for {new Date().toLocaleString('default', { month: 'long', year: 'numeric' })} to receive accurate AI financial insights.</p>
            </div>

            <form onSubmit={handleIncomeSubmit} className="space-y-6">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Monthly Income (₹)</label>
                <input
                  type="number"
                  value={modalIncome}
                  onChange={(e) => setModalIncome(e.target.value)}
                  placeholder="e.g. 50000"
                  required
                  className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-lg font-medium focus:outline-none focus:border-[#00d4ff]"
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Salary Date</label>
                <input
                  type="number"
                  min="1"
                  max="31"
                  value={modalSalaryDate}
                  onChange={(e) => setModalSalaryDate(e.target.value)}
                  required
                  className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-lg font-medium focus:outline-none focus:border-[#00d4ff]"
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Income Type (Optional)</label>
                <select
                  value={modalIncomeType}
                  onChange={(e) => setModalIncomeType(e.target.value)}
                  className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-lg font-medium focus:outline-none focus:border-[#00d4ff]"
                >
                  <option value="">Select Income Type...</option>
                  <option value="Salary Increment">Salary Increment</option>
                  <option value="Bonus">Bonus</option>
                  <option value="Freelancing">Freelancing</option>
                  <option value="Business Income">Business Income</option>
                  <option value="Other">Other</option>
                </select>
              </div>
              <div className="flex gap-4">
                <button
                  type="button"
                  onClick={() => { setShowIncomeModal(false); setShowIncomeBanner(true); }}
                  className="flex-1 bg-slate-800 text-white py-4 rounded-xl font-bold text-lg hover:bg-slate-700 transition-colors"
                >
                  Later
                </button>
                <button
                  type="submit"
                  className="flex-[2] bg-gradient-to-r from-[#00d4ff] to-[#8b5cf6] text-white py-4 rounded-xl font-bold text-lg hover:opacity-90 transition-opacity"
                >
                  Save Income
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {notifyMessage && (
        <div className={`toast toast-${notifyMessage.type}`}>
          {notifyMessage.type === 'success' ? (
            <Activity className="toast-icon" />
          ) : (
            <AlertTriangle className="toast-icon" />
          )}
          <span className="toast-text">{notifyMessage.text}</span>
        </div>
      )}

      {/* Sidebar Navigation */}
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-brand-icon">
            <Compass />
          </div>
          <div>
            <h1 className="sidebar-brand-title">FinPilot AI</h1>
            <span className="sidebar-brand-badge">Financial Coach</span>
          </div>
        </div>

        {/* User Workspace Profile Card */}
        <div className="sidebar-profile">
          <div className="sidebar-profile-content">
            <div className="sidebar-profile-avatar">
              {name.charAt(0)}
            </div>
            <div className="sidebar-profile-info">
              <p className="sidebar-profile-name">{name}</p>
              <p className="sidebar-profile-email">{email}</p>
            </div>
          </div>
        </div>

        {/* Tab Selection Navigation */}
        <nav className="sidebar-nav">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: Activity },
            { id: 'simulator', label: 'Simulator Sandbox', icon: Sparkles },
            { id: 'chatbot', label: 'Coach Chat', icon: MessageSquare },
            { id: 'settings', label: 'Settings', icon: Settings }
          ].map(tab => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`sidebar-nav-btn ${activeTab === tab.id ? 'sidebar-nav-btn-active' : ''}`}
              >
                <Icon />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="sidebar-logout">
          <button onClick={handleLogout} className="sidebar-logout-btn">
            <LogOut />
            <span>Logout Account</span>
          </button>
        </div>
      </aside>

      {/* Main Dynamic Panel Wrapper */}
      <main className="main-content">
        {loading && (
          <div className="loading-overlay">
            <div className="loading-spinner">
              <div className="loading-spinner-ring" />
              <span className="loading-spinner-text">Syncing Ledger...</span>
            </div>
          </div>
        )}

        {/* --- VIEW TAB A: DASHBOARD HUB --- */}
        {activeTab === 'dashboard' && (
          <div className="space-y-6">
            <div className="dashboard-header">
              <div>
                <h2 className="dashboard-title">🎉Welcome!</h2>
                <p className="dashboard-subtitle">Real-time health score, smart transaction processing & metrics</p>
              </div>

              <div className="dashboard-actions">
                <button
                  onClick={handleVoiceRecordSimulate}
                  className={`dashboard-action-btn ${isRecording ? 'dashboard-action-btn-recording' : ''}`}
                >
                  <Mic />
                  <span>{isRecording ? 'Recording...' : 'Voice Entry'}</span>
                </button>
                <label className="dashboard-action-btn dashboard-action-btn-upload">
                  <FileText />
                  <span>Upload Receipt</span>
                  <input type="file" accept="image/*" onChange={handleReceiptUpload} />
                </label>
              </div>
            </div>

            <form onSubmit={submitRawExpense} className="expense-input-bar">
              <Sparkles />
              <input
                type="text"
                placeholder='Enter raw expense, e.g., "₹250 Pizza 2:20 PM" or "Starbucks coffee 150 rupees"...'
                value={rawText}
                onChange={(e) => setRawText(e.target.value)}
              />
              {rawText && (
                <button type="submit" className="expense-input-bar-submit">
                  Log AI Expense
                </button>
              )}
            </form>

            {voiceText && (
              <div className="voice-text-display">
                <Info />
                <span>{voiceText}</span>
              </div>
            )}

            {/* Banner if Later is clicked */}
            {showIncomeBanner && (
              <div className="bg-orange-500/10 border border-orange-500/20 text-orange-400 p-4 rounded-xl flex items-center justify-between animate-in fade-in">
                <div className="flex items-center gap-3">
                  <AlertTriangle size={20} />
                  <span>You haven't entered your income for {new Date().toLocaleString('default', { month: 'long' })}. Please update it to receive accurate AI financial insights.</span>
                </div>
                <button onClick={() => setShowIncomeModal(true)} className="bg-orange-500/20 px-4 py-2 rounded-lg font-medium hover:bg-orange-500/30 transition-colors">
                  Enter Income
                </button>
              </div>
            )}

            {/* Financial Overview Cards */}
            <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
              {/* Income Card */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 relative col-span-1 flex flex-col justify-between">
                <div>
                  <h3 className="text-slate-400 font-semibold mb-2 uppercase tracking-wider text-xs">Monthly Income</h3>
                  <div className="text-3xl font-bold text-[#00d4ff] mb-1">
                    ₹{dashboard?.monthlyIncomeBudget || '0'}
                  </div>
                  <div className="text-sm font-medium text-slate-300">{new Date().toLocaleString('default', { month: 'long', year: 'numeric' })}</div>
                  {dashboard?.monthlyIncomeBudget > 0 ? (
                    <>
                      <div className="text-[10px] text-slate-500 mt-4 uppercase tracking-wider font-semibold">Last Updated</div>
                      <div className="text-xs text-slate-400">{dashboard?.monthlyIncomeLastUpdated ? new Date(dashboard.monthlyIncomeLastUpdated).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Never'}</div>
                    </>
                  ) : (
                    <div className="mt-4 text-xs text-orange-400 font-medium">⚠ No income set for this month</div>
                  )}
                </div>
                <button onClick={() => setShowIncomeModal(true)} className="mt-5 w-full bg-slate-800 hover:bg-slate-700 text-white py-2.5 rounded-xl text-sm font-bold transition-colors border border-slate-700">
                  {dashboard?.monthlyIncomeBudget > 0 ? 'Edit Income' : '+ Add Income'}
                </button>
              </div>

              {/* Other Cards */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col justify-center">
                <h3 className="text-slate-400 font-semibold mb-2 uppercase tracking-wider text-xs">Total Expenses</h3>
                <div className="text-3xl font-bold text-[#ef4444]">₹{dashboard?.monthlySpent || '0'}</div>
                {dashboard?.totalTransactionCount > 0 && (
                  <div className="text-xs text-slate-500 mt-2">{dashboard.totalTransactionCount} transaction{dashboard.totalTransactionCount !== 1 ? 's' : ''}</div>
                )}
              </div>
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col justify-center">
                <h3 className="text-slate-400 font-semibold mb-2 uppercase tracking-wider text-xs">Savings</h3>
                {dashboard?.monthlyIncomeBudget > 0 ? (
                  <div className={`text-3xl font-bold ${dashboard.monthlySavings >= 0 ? 'text-[#10b981]' : 'text-[#ef4444]'}`}>₹{dashboard.monthlySavings || '0'}</div>
                ) : (
                  <div className="text-2xl font-bold text-slate-500">N/A</div>
                )}
              </div>
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col justify-center">
                <h3 className="text-slate-400 font-semibold mb-2 uppercase tracking-wider text-xs">Savings Rate</h3>
                {dashboard?.savingsRate != null ? (
                  <div className="text-3xl font-bold text-white">{dashboard.savingsRate}%</div>
                ) : (
                  <div className="text-2xl font-bold text-slate-500">N/A</div>
                )}
                {dashboard?.savingsRate == null && <div className="text-xs text-slate-600 mt-1">Add income to track</div>}
              </div>
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col justify-center">
                <h3 className="text-slate-400 font-semibold mb-2 uppercase tracking-wider text-xs">Financial Health</h3>
                {dashboard?.healthScore != null ? (
                  <div className="text-3xl font-bold text-[#8b5cf6]">{dashboard.healthScore} / 100</div>
                ) : (
                  <div className="text-2xl font-bold text-slate-500">N/A</div>
                )}
                {dashboard?.healthScore == null && <div className="text-xs text-slate-600 mt-1">Needs 10+ transactions</div>}
              </div>
            </div>

            <div className="dashboard-grid">
              <div className="dashboard-card">
                <div className="health-score-container">
                  <div>
                    <h3 className="dashboard-card-label">Financial Health Score</h3>
                    {dashboard?.healthScore != null ? (
                      <p className="dashboard-card-sub">{dashboard.healthScoreExplanation}</p>
                    ) : (
                      <p className="dashboard-card-sub text-slate-600">
                        {dashboard?.totalTransactionCount >= 10
                          ? 'Set your income to unlock your score.'
                          : `Add ${Math.max(0, 10 - (dashboard?.totalTransactionCount || 0))} more transaction${(10 - (dashboard?.totalTransactionCount || 0)) !== 1 ? 's' : ''} to unlock AI insights.`
                        }
                      </p>
                    )}
                  </div>
                  <div className="health-score-ring">
                    <svg viewBox="0 0 80 80">
                      <circle cx="40" cy="40" r="34" className="health-score-ring-bg" />
                      <circle
                        cx="40"
                        cy="40"
                        r="34"
                        className="health-score-ring-fg"
                        strokeDasharray="213.6"
                        strokeDashoffset={213.6 - (213.6 * (dashboard?.healthScore || 0)) / 100}
                        style={{ opacity: dashboard?.healthScore != null ? 1 : 0.15 }}
                      />
                    </svg>
                    <span className="health-score-number" style={{ color: dashboard?.healthScore != null ? undefined : '#475569' }}>
                      {dashboard?.healthScore != null ? dashboard.healthScore : '--'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="dashboard-card dashboard-card-full">
                <div className="personality-container">
                  <div className="personality-icon">
                    <Compass />
                  </div>
                  <div>
                    <div className="personality-header">
                      <h3 className="dashboard-card-label">AI Spending Personality</h3>
                      {dashboard?.personalityType ? (
                        <span className="personality-badge">{dashboard.personalityType}</span>
                      ) : (
                        <span className="personality-badge" style={{ opacity: 0.5 }}>Pending</span>
                      )}
                    </div>
                    {dashboard?.personalityExplanation ? (
                      <p className="personality-text">{dashboard.personalityExplanation}</p>
                    ) : (
                      <p className="personality-text text-slate-600">
                        Your spending personality will be revealed after you log at least 10 transactions. Start by adding your first expense using the input bar above.
                      </p>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Lifestyle Spending Analysis Section */}
            <div className="dashboard-grid">
              <div className="dashboard-card dashboard-card-full bg-gradient-to-br from-slate-900 via-purple-950/20 to-slate-900 border border-slate-800/80 rounded-2xl p-6 animate-in fade-in slide-in-from-bottom duration-300">
                <h3 className="dashboard-card-label flex items-center gap-2 mb-4 text-[#8b5cf6] text-lg font-bold">
                  <Brain size={22} className="text-[#8b5cf6] animate-pulse" />
                  <span>🧠 Lifestyle Spending Analysis</span>
                </h3>

                {!dashboard?.hasEnoughData ? (
                  <div className="text-center py-10">
                    <div className="text-4xl mb-3">📊</div>
                    <p className="font-semibold text-slate-300 mb-2">Not enough data yet</p>
                    <p className="text-sm text-slate-500 max-w-sm mx-auto">
                      Your personalized AI analysis unlocks after you log at least <span className="text-[#8b5cf6] font-bold">10 transactions</span> and set your monthly income.
                      You currently have <span className="text-white font-bold">{dashboard?.totalTransactionCount || 0}</span> transaction{(dashboard?.totalTransactionCount || 0) !== 1 ? 's' : ''}.
                    </p>
                    <button
                      onClick={() => document.querySelector('.expense-input-bar input')?.focus()}
                      className="mt-5 bg-[#8b5cf6]/20 border border-[#8b5cf6]/30 text-[#c084fc] px-6 py-2.5 rounded-xl text-sm font-semibold hover:bg-[#8b5cf6]/30 transition-colors"
                    >
                      + Add Your First Expense
                    </button>
                  </div>
                ) : dashboard?.lifestyleAnalysis ? (
                  <div className="space-y-6 animate-in fade-in duration-500">
                    {/* Archetype Badge */}
                    <div className="flex items-center gap-3 bg-slate-800/40 p-3.5 rounded-xl border border-slate-700/30 w-fit">
                      <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Spending Personality:</span>
                      <span className="bg-[#8b5cf6]/20 text-[#c084fc] px-3 py-1 rounded-full text-xs font-bold border border-[#8b5cf6]/30">
                        {dashboard.lifestyleAnalysis.personalityType}
                      </span>
                      <span className="text-xs text-slate-500">|</span>
                      <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Wellness Score:</span>
                      <span className="text-emerald-400 font-bold text-xs">{dashboard.lifestyleAnalysis.wellnessScore}/100</span>
                    </div>

                    {/* AI Summary */}
                    <div className="space-y-2">
                      <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                        <div className="w-1.5 h-1.5 rounded-full bg-[#8b5cf6]" />
                        AI Summary
                      </h4>
                      <p className="text-sm text-slate-300 leading-relaxed bg-slate-950/50 p-4 rounded-xl border border-slate-850">
                        {dashboard.lifestyleAnalysis.aiSummary}
                      </p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      {/* Positive Financial Habits */}
                      <div className="space-y-2">
                        <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                          <div className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                          Positive Financial Habits
                        </h4>
                        <div className="text-sm text-slate-300 bg-emerald-500/5 border border-emerald-500/10 p-4 rounded-xl leading-relaxed flex items-start gap-2.5">
                          <span className="text-emerald-400 text-base font-bold">✅</span>
                          <span>{dashboard.lifestyleAnalysis.positiveHabit}</span>
                        </div>
                      </div>

                      {/* Areas That Need Improvement */}
                      <div className="space-y-2">
                        <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                          <div className="w-1.5 h-1.5 rounded-full bg-orange-400" />
                          Areas That Need Improvement
                        </h4>
                        <div className="text-sm text-slate-300 bg-orange-500/5 border border-orange-500/10 p-4 rounded-xl leading-relaxed flex items-start gap-2.5">
                          <span className="text-orange-400 text-base font-bold">⚠</span>
                          <span>{dashboard.lifestyleAnalysis.improvementSuggestion}</span>
                        </div>
                      </div>
                    </div>

                    {/* AI Recommendation */}
                    <div className="space-y-2 pt-2 border-t border-slate-800/80">
                      <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                        <div className="w-1.5 h-1.5 rounded-full bg-[#10b981]" />
                        AI Recommendation
                      </h4>
                      <div className="text-sm text-slate-300 bg-slate-950/40 p-4 rounded-xl border border-slate-900 flex items-start gap-2.5">
                        <span className="text-[#10b981] text-base font-bold">💰</span>
                        <div>
                          <span className="italic">"{dashboard.lifestyleAnalysis.savingsOpportunity}"</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-8 text-slate-500">
                    <p className="font-medium text-slate-400 mb-1">Generating your analysis...</p>
                    <p className="text-xs">This may take a moment on first load.</p>
                  </div>
                )}
              </div>
            </div>

            <div className="dashboard-grid">
              <div className="dashboard-card dashboard-card-full">
                <div className="monthly-story">
                  <div>
                    <h3 className="dashboard-card-label mb-3">AI Monthly Story Summary</h3>
                    {dashboard?.monthlyStory ? (
                      <p className="monthly-story-text">"{dashboard.monthlyStory}"</p>
                    ) : (
                      <p className="monthly-story-text" style={{ color: '#475569', fontStyle: 'normal' }}>
                        Your monthly story appears here once you've added income and started logging transactions. Use the expense input bar above to get started.
                      </p>
                    )}
                  </div>
                  <div className="monthly-story-meta">
                    <span>💼 Monthly Income Budget: ₹{dashboard?.monthlyIncomeBudget || '0'}</span>
                    <span>🎯 Goals Active: {dashboard?.goalsCount || '0'}</span>
                  </div>
                </div>
              </div>
            </div>
            <div className="dashboard-grid">
              <div className="dashboard-card dashboard-card-full">
                <h3 className="dashboard-card-label mb-4">Weekly Spending Outflow (INR)</h3>
                <div className="chart-container flex items-center justify-center">
                  {dashboard?.weeklyTrends?.some(t => t.amount > 0) ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={dashboard.weeklyTrends}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#ffffff0d" />
                        <XAxis dataKey="day" tick={{ fill: '#94a3b8', fontSize: 10 }} axisLine={false} />
                        <YAxis tick={{ fill: '#94a3b8', fontSize: 10 }} axisLine={false} />
                        <Tooltip contentStyle={{ backgroundColor: '#0a0c12', borderColor: '#ffffff1a' }} />
                        <Bar dataKey="amount" fill="#00d4ff" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="text-center text-slate-500 py-8">
                      <p className="font-medium text-slate-400 mb-1">No expenses recorded yet.</p>
                      <p className="text-sm">Add your first expense to view spending trends.</p>
                    </div>
                  )}
                </div>
              </div>

              <div className="dashboard-card">
                <div>
                  <h3 className="dashboard-card-label mb-4">Spending Distribution</h3>
                  <div className="pie-chart-container flex items-center justify-center">
                    {dashboard?.categoryBreakdown && dashboard.categoryBreakdown.length > 0 ? (
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={dashboard.categoryBreakdown}
                            cx="50%"
                            cy="50%"
                            innerRadius={45}
                            outerRadius={65}
                            paddingAngle={4}
                            dataKey="value"
                          >
                            {dashboard.categoryBreakdown.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip contentStyle={{ backgroundColor: '#0a0c12', borderColor: '#ffffff1a' }} />
                        </PieChart>
                      </ResponsiveContainer>
                    ) : (
                      <div className="text-center text-slate-500 py-8">
                        <p className="font-medium text-slate-400 mb-1">No categorizations recorded.</p>
                        <p className="text-sm">Expenses will appear here.</p>
                      </div>
                    )}
                  </div>
                </div>
                <div className="chart-legend">
                  {dashboard?.categoryBreakdown?.map((entry, index) => (
                    <div key={entry.name} className="chart-legend-item">
                      <div className="chart-legend-dot" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                      <span className="text-slate-400 truncate">{entry.name}</span>
                      <span className="text-slate-500 font-bold">₹{entry.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="dashboard-grid">
              <div className="dashboard-card">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="dashboard-card-label">Goals Roadmap & Forecast</h3>
                  <button
                    onClick={() => setActiveTab('simulator')}
                    className="text-xs text-[#00d4ff] flex items-center gap-1 hover:underline"
                  >
                    <span>Manage Goals</span>
                    <ArrowRight size={14} />
                  </button>
                </div>
                <div className="goals-container">
                  {goals.map(goal => {
                    const progress = goal.targetAmount > 0 ? Math.min(100, Math.floor((goal.currentAmount / goal.targetAmount) * 100)) : 0;
                    return (
                      <div key={goal.id} className="goal-item">
                        <div className="goal-header">
                          <span className="goal-name">{goal.name}</span>
                          <span className="goal-amount">₹{goal.currentAmount} / ₹{goal.targetAmount}</span>
                        </div>
                        <div className="goal-progress">
                          <div className="goal-progress-bar" style={{ width: `${progress}%` }} />
                        </div>
                        <div className="goal-meta">
                          <span className="goal-meta-left">
                            <Clock />
                            <span>Proj. Complete: {goal.predictedCompletionDate || 'Calculating'}</span>
                          </span>
                          <span className={`goal-status ${goal.status === 'DELAYED' ? 'goal-status-delayed' : 'goal-status-on-track'}`}>
                            {goal.status}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                  {goals.length === 0 && <p className="text-xs text-slate-500">No active goals found. Create targets in the Simulator Sandbox.</p>}
                </div>
              </div>

              <div className="dashboard-card">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="dashboard-card-label">Subscription & Recurring Detector</h3>
                  {dashboard?.flaggedSubscriptionsCount > 0 && (
                    <span className="px-2 py-0.5 rounded bg-orange-500/20 text-orange-400 text-[10px] font-bold flex items-center gap-1">
                      <AlertTriangle size={12} />
                      <span>{dashboard.flaggedSubscriptionsCount} Unused Leaks</span>
                    </span>
                  )}
                </div>
                <div className="overflow-x-auto">
                  <table className="subscription-table">
                    <thead>
                      <tr>
                        <th>Merchant</th>
                        <th>Amount</th>
                        <th className="subscription-flag">Auto-Flag</th>
                        <th className="subscription-action">Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {subscriptions.map(sub => (
                        <tr key={sub.id}>
                          <td className="subscription-merchant">{sub.merchant}</td>
                          <td className="subscription-amount">₹{sub.amount} / mo</td>
                          <td className="subscription-flag">
                            <button
                              onClick={() => toggleSubUnused(sub.id, sub.isFlaggedUnused)}
                              className={`subscription-flag-btn ${sub.isFlaggedUnused ? 'subscription-flag-btn-unused' : 'subscription-flag-btn-clear'}`}
                            >
                              {sub.isFlaggedUnused ? 'Unused Leak' : '--'}
                            </button>
                          </td>
                          <td className="subscription-action">
                            <button
                              onClick={() => toggleSub(sub.id)}
                              className={`subscription-action-btn ${sub.isActive ? 'subscription-action-btn-active' : 'subscription-action-btn-renew'}`}
                            >
                              {sub.isActive ? 'Cancel' : 'Renew'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {subscriptions.length === 0 && <p className="text-xs text-slate-500 mt-2">No subscription bills processed.</p>}
                </div>
              </div>
            </div>

            <div className="dashboard-card">
              <h3 className="dashboard-card-label mb-4">Smart Ledger Context Cards</h3>
              <div className="expense-cards">
                {expenses.slice(0, 8).map(exp => (
                  <div key={exp.id} className="expense-card">
                    <div className="expense-card-header">
                      <div>
                        <h4 className="expense-card-merchant">{exp.merchant || 'General Expense'}</h4>
                        <p className="expense-card-date">{exp.transactionDate} at {exp.transactionTime || '12:00 PM'}</p>
                      </div>
                      <div className="text-right">
                        <span className="expense-card-amount">₹{exp.amount}</span>
                        {exp.gst > 0 && <p className="expense-card-gst">GST ₹{exp.gst}</p>}
                      </div>
                    </div>
                    <div className="expense-card-tags">
                      <span className="expense-card-tag expense-card-tag-category">{exp.category}</span>
                      <span className={`expense-card-tag ${exp.expenseType === 'Essential' ? 'expense-card-tag-essential' : 'expense-card-tag-nonessential'}`}>
                        {exp.expenseType}
                      </span>
                      {exp.mealType && exp.mealType !== 'None' && (
                        <span className="expense-card-tag expense-card-tag-meal">
                          <Clock />
                          <span>{exp.mealType}</span>
                        </span>
                      )}
                    </div>
                    {exp.aiContextSummary && (
                      <p className="expense-card-context">🤖 {exp.aiContextSummary}</p>
                    )}
                  </div>
                ))}
                {expenses.length === 0 && <p className="text-xs text-slate-500 col-span-2">No expenses available in ledger.</p>}
              </div>
            </div>
          </div>
        )}

        {/* --- VIEW TAB B: SIMULATOR SANDBOX --- */}
        {activeTab === 'simulator' && (
          <div className="space-y-6">
            <div>
              <h2 className="dashboard-title">Financial Projections & Simulator Sandbox</h2>
              <p className="dashboard-subtitle">Project buying timelines or calculate the value of behavioral shifts</p>
            </div>

            <div className="simulator-grid">
              <div className="simulator-card">
                <form onSubmit={handleCreateGoal} className="simulator-form">
                  <h3 className="simulator-card-title">Plan Financial Goal</h3>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Goal Identifier</label>
                    <input
                      type="text"
                      className="simulator-form-input"
                      placeholder="e.g. Electric Bike, iPad Pro"
                      value={newGoalName}
                      onChange={(e) => setNewGoalName(e.target.value)}
                      required
                    />
                  </div>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Target Amount (₹)</label>
                    <input
                      type="number"
                      className="simulator-form-input"
                      placeholder="e.g. 80000"
                      value={newGoalTarget}
                      onChange={(e) => setNewGoalTarget(e.target.value)}
                      required
                    />
                  </div>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Target Date</label>
                    <input
                      type="date"
                      className="simulator-form-input"
                      value={newGoalDate}
                      onChange={(e) => setNewGoalDate(e.target.value)}
                    />
                  </div>
                  <button type="submit" className="simulator-form-btn simulator-form-btn-gradient">
                    <Plus size={16} />
                    <span>Establish Goal</span>
                  </button>
                </form>
              </div>

              <div className="simulator-card">
                <h3 className="simulator-card-title">Active Savings Roadmap</h3>
                <div className="goals-list">
                  {goals.map(g => (
                    <div key={g.id} className="goals-list-item">
                      <div className="goals-list-info">
                        <h4 className="goals-list-name">{g.name}</h4>
                        <p className="goals-list-amount">Target Amount: ₹{g.targetAmount} (Currently funded: ₹{g.currentAmount})</p>
                      </div>
                      <div className="goals-list-actions">
                        <button
                          onClick={() => contributeGoal(g.id, 1000)}
                          className="goals-list-contribute goals-list-contribute-1k"
                        >
                          + ₹1,000
                        </button>
                        <button
                          onClick={() => contributeGoal(g.id, 5000)}
                          className="goals-list-contribute goals-list-contribute-5k"
                        >
                          + ₹5,000
                        </button>
                      </div>
                    </div>
                  ))}
                  {goals.length === 0 && <p className="simulator-empty">No targets active. Set up a target parameters card to begin forecast calculations.</p>}
                </div>
              </div>
            </div>

            <div className="simulator-grid">
              <div className="simulator-card">
                <form onSubmit={runPurchaseSimulation} className="simulator-form">
                  <h3 className="simulator-card-title">Future Purchase Simulator</h3>
                  <p className="simulator-card-subtitle">Calculate how this purchase alters active goals timelines.</p>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Item Title</label>
                    <input
                      type="text"
                      className="simulator-form-input"
                      value={simName}
                      onChange={(e) => setSimName(e.target.value)}
                      required
                    />
                  </div>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Total Cost (₹)</label>
                    <input
                      type="number"
                      className="simulator-form-input"
                      value={simCost}
                      onChange={(e) => setSimCost(e.target.value)}
                      required
                    />
                  </div>
                  <button type="submit" className="simulator-form-btn simulator-form-btn-primary">
                    <Zap size={16} />
                    <span>Run Simulation Futures</span>
                  </button>
                </form>
              </div>

              <div className="simulator-card">
                <h3 className="simulator-card-title">Multi-Scenario Forecast Outcomes</h3>
                {simLoading ? (
                  <div className="simulator-loading">
                    <div className="simulator-loading-spinner" />
                    <span className="simulator-loading-text">AI mapping future scenarios...</span>
                  </div>
                ) : simResult ? (
                  <div className="simulator-results">
                    <div className="simulator-recommendation">
                      🤖 <strong>AI Coach Recommendation:</strong> {simResult.recommendation}
                    </div>
                    <div className="simulator-scenarios">
                      {['scenarioA', 'scenarioB', 'scenarioC', 'scenarioD'].map((scKey) => {
                        const scenario = simResult[scKey];
                        if (!scenario) return null;
                        return (
                          <div key={scKey} className="simulator-scenario">
                            <div className="simulator-scenario-header">
                              <span className="simulator-scenario-name">{scenario.name}</span>
                              <span className="simulator-scenario-cost">₹{scenario.totalCost || scenario.monthlyCost}</span>
                            </div>
                            <p className="simulator-scenario-desc">{scenario.description}</p>
                            {scenario.goalDelayMonths !== undefined && (
                              <p className={`simulator-scenario-impact ${scenario.goalDelayMonths > 0 ? 'simulator-scenario-impact-delay' : 'simulator-scenario-impact-ok'}`}>
                                {scenario.goalDelayMonths > 0 ? `⚠️ Delay: ${scenario.goalDelayMonths} months` : '✅ No Forecast Delay'}
                              </p>
                            )}
                            {scenario.impactAnalysis && <p className="simulator-scenario-desc">{scenario.impactAnalysis}</p>}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ) : (
                  <div className="simulator-empty">Input planned items and cost dimensions to simulate scenario trajectories.</div>
                )}
              </div>
            </div>

            <div className="simulator-grid">
              <div className="simulator-card">
                <form onSubmit={runWhatIfSimulation} className="simulator-form">
                  <h3 className="simulator-card-title">Behavioral "What-If" Simulator</h3>
                  <p className="simulator-card-subtitle">Model the long-term compound savings of reducing regular expenditures.</p>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Expense Type</label>
                    <input
                      type="text"
                      className="simulator-form-input"
                      value={whatIfTarget}
                      onChange={(e) => setWhatIfTarget(e.target.value)}
                      required
                    />
                  </div>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Cost per Incident (₹)</label>
                    <input
                      type="number"
                      className="simulator-form-input"
                      value={whatIfAmount}
                      onChange={(e) => setWhatIfAmount(e.target.value)}
                      required
                    />
                  </div>
                  <div className="simulator-form-group">
                    <label className="simulator-form-label">Incidents per Week</label>
                    <input
                      type="number"
                      className="simulator-form-input"
                      value={whatIfFreq}
                      onChange={(e) => setWhatIfFreq(e.target.value)}
                      required
                    />
                  </div>
                  <button type="submit" className="simulator-form-btn simulator-form-btn-gradient">
                    <Compass size={16} />
                    <span>Project Sandbox Projections</span>
                  </button>
                </form>
              </div>

              <div className="simulator-card">
                <h3 className="simulator-card-title">Compounding Savings Timeline</h3>
                {whatIfResult ? (
                  <div className="space-y-6">
                    <div className="whatif-grid">
                      {[
                        { label: 'Monthly Savings', val: whatIfResult.monthlySavings, color: 'whatif-value-cyan' },
                        { label: '6-Month Savings', val: whatIfResult.savings6m, color: 'whatif-value-purple' },
                        { label: '1-Year Savings', val: whatIfResult.savings1y, color: 'whatif-value-emerald' },
                        { label: '5-Year Growth (8%)', val: whatIfResult.savings5y, color: 'whatif-value-emerald', glow: true }
                      ].map((card, i) => (
                        <div key={i} className={`whatif-card ${card.glow ? 'whatif-card-glow' : ''}`}>
                          <span className="whatif-label">{card.label}</span>
                          <strong className={`whatif-value ${card.color}`}>₹{card.val}</strong>
                        </div>
                      ))}
                    </div>
                    <div className="whatif-explanation">
                      🤖 {whatIfResult.explanation}
                    </div>
                  </div>
                ) : (
                  <div className="simulator-empty">Define behavior constraints to view compound metrics projections.</div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* --- VIEW TAB C: CONVERSATIONAL AI COACH --- */}
        {activeTab === 'chatbot' && (
          <div className="chatbot-container">
            <div className="chatbot-header">
              <h2 className="chatbot-title">AI Financial Coach</h2>
              <p className="chatbot-subtitle">Converse naturally regarding salary leaks, categories, and financial milestones</p>
            </div>

            <div className="chatbot-box">
              <div className="chatbot-messages">
                {chatLog.map((logItem, idx) => (
                  <div key={idx} className={`chatbot-message ${logItem.sender === 'user' ? 'chatbot-message-user' : 'chatbot-message-ai'}`}>
                    <div className={`chatbot-bubble ${logItem.sender === 'user' ? 'chatbot-bubble-user' : 'chatbot-bubble-ai'}`}>
                      {logItem.sender === 'ai' && (
                        <div className="chatbot-ai-header">
                          <Compass />
                          <span>FinPilot AI Coach</span>
                        </div>
                      )}
                      <p className="whitespace-pre-line">{logItem.text}</p>
                    </div>
                  </div>
                ))}

                {chatLoading && (
                  <div className="chatbot-typing">
                    <div className="chatbot-typing-dots">
                      <div className="chatbot-typing-dot" />
                      <div className="chatbot-typing-dot" />
                      <div className="chatbot-typing-dot" />
                    </div>
                    <span>Formulating advisory guidelines...</span>
                  </div>
                )}
                <div ref={chatEndRef} />
              </div>

              <form onSubmit={submitChat} className="chatbot-input">
                <input
                  type="text"
                  className="chatbot-input-field"
                  placeholder="Ask, e.g., 'Where did my salary go?' or 'Can I buy a laptop next month?'..."
                  value={chatMessage}
                  onChange={(e) => setChatMessage(e.target.value)}
                />
                <button type="submit" className="chatbot-send-btn">
                  <MessageSquare />
                </button>
              </form>
            </div>
          </div>
        )}

        {/* --- VIEW TAB D: SETTINGS --- */}
        {activeTab === 'settings' && (
          <SettingsDashboard
            token={token}
            onLogout={handleLogout}
            showNotification={showNotification}
          />
        )}
      </main>
    </div>
  );
}