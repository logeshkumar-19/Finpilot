import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  User, Shield, Cpu, Target, Bell, Palette, Download, Database, Info, LogOut, Save
} from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export default function SettingsDashboard({ token, onLogout, showNotification }) {
  const [activeTab, setActiveTab] = useState('profile');
  const [profileData, setProfileData] = useState(null);
  const [preferences, setPreferences] = useState(null);
  const [loading, setLoading] = useState(true);

  // Security Form State
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const api = axios.create({
    baseURL: API_BASE,
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });

  useEffect(() => {
    fetchSettingsData();
  }, [token]);

  const fetchSettingsData = async () => {
    setLoading(true);
    try {
      const [profRes, prefRes] = await Promise.all([
        api.get('/settings/profile'),
        api.get('/settings/preferences')
      ]);
      setProfileData(profRes.data);
      setPreferences(prefRes.data);
    } catch (err) {
      console.error('Error fetching settings:', err);
      showNotification('Failed to load settings.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    try {
      await api.put('/settings/profile', {
        firstName: profileData.firstName,
        lastName: profileData.lastName,
        profilePicture: profileData.profilePicture
      });
      showNotification('Profile updated successfully!', 'success');
    } catch (err) {
      showNotification('Failed to update profile.', 'error');
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    try {
      await api.put('/settings/change-password', {
        currentPassword, newPassword, confirmPassword
      });
      showNotification('Password updated securely.', 'success');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      showNotification(err.response?.data || 'Failed to update password.', 'error');
    }
  };

  const handleUpdatePreference = async (key, value) => {
    const updatedPrefs = { ...preferences, [key]: value };
    setPreferences(updatedPrefs);
    try {
      await api.put('/settings/preferences', updatedPrefs);
      showNotification('Preference saved automatically.', 'success');
    } catch (err) {
      showNotification('Failed to save preference.', 'error');
    }
  };

  const handleExport = async (format) => {
    try {
      await api.get('/settings/export');
      showNotification(`Your ${format} report is downloading...`, 'success');
    } catch (err) {
      showNotification('Export failed.', 'error');
    }
  };

  const handleDeleteAccount = async () => {
    if (window.confirm("Are you absolutely sure you want to delete your account? This action is irreversible.")) {
      try {
        await api.delete('/settings/account');
        showNotification('Account deleted.', 'success');
        onLogout();
      } catch (err) {
        showNotification('Failed to delete account.', 'error');
      }
    }
  };

  if (loading || !profileData || !preferences) {
    return (
      <div className="flex justify-center items-center p-16">
        <div className="animate-pulse text-slate-400 text-lg">Loading Control Center...</div>
      </div>
    );
  }

  const tabs = [
    { id: 'profile', icon: User, label: 'Profile' },
    { id: 'security', icon: Shield, label: 'Security' },
    { id: 'ai', icon: Cpu, label: 'AI Preferences' },
    { id: 'financial', icon: Target, label: 'Financial Preferences' },
    { id: 'notifications', icon: Bell, label: 'Notifications' },
    { id: 'appearance', icon: Palette, label: 'Appearance' },
    { id: 'export', icon: Download, label: 'Export Data' },
    { id: 'privacy', icon: Database, label: 'Data & Privacy' },
    { id: 'about', icon: Info, label: 'About' },
  ];

  return (
    <div className="settings-container flex h-full">
      {/* Settings Sidebar */}
      <div className="settings-sidebar w-80 border-r border-slate-800/80 p-8 shrink-0 flex flex-col bg-slate-950/30">
        <h2 className="text-2xl font-bold text-white mb-10 px-2 tracking-tight">Control Center</h2>
        <nav className="flex flex-col gap-3 flex-1">
          {tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-4 px-5 py-4 rounded-2xl text-base font-medium transition-all duration-300 ${
                activeTab === tab.id 
                  ? 'bg-slate-800 text-[#00d4ff] shadow-md' 
                  : 'text-slate-400 hover:bg-slate-800/60 hover:text-white'
              }`}
            >
              <tab.icon size={22} className={activeTab === tab.id ? "text-[#00d4ff]" : "text-slate-500"} />
              <span>{tab.label}</span>
            </button>
          ))}
          
          <div className="mt-auto pt-8">
            <div className="mb-8 border-t border-slate-800" />
            <button
              onClick={() => {
                if (window.confirm("Are you sure you want to logout?")) onLogout();
              }}
              className="w-full flex items-center gap-4 px-5 py-4 rounded-2xl text-base font-medium text-red-400 hover:bg-red-500/10 transition-colors"
            >
              <LogOut size={22} />
              <span>Logout</span>
            </button>
          </div>
        </nav>
      </div>

      {/* Settings Content Area */}
      <div className="settings-content flex-1 p-12 overflow-y-auto bg-transparent">
        
        {/* Profile Tab */}
        {activeTab === 'profile' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-5xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">Personal Information</h3>
              <p className="text-lg text-slate-400">Manage your basic profile details and view your financial trajectory.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-8 border border-slate-800/80 shadow-lg shadow-black/20">
              <form onSubmit={handleUpdateProfile} className="space-y-8">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">First Name</label>
                    <input
                      type="text"
                      value={profileData.firstName}
                      onChange={(e) => setProfileData({ ...profileData, firstName: e.target.value })}
                      className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#00d4ff] focus:bg-slate-800 transition-colors"
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Last Name</label>
                    <input
                      type="text"
                      value={profileData.lastName}
                      onChange={(e) => setProfileData({ ...profileData, lastName: e.target.value })}
                      className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#00d4ff] focus:bg-slate-800 transition-colors"
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Email Address (Locked)</label>
                    <input
                      type="email"
                      value={profileData.email}
                      disabled
                      className="w-full bg-slate-800/30 border border-slate-700/50 rounded-xl px-5 py-3.5 text-slate-500 cursor-not-allowed text-base"
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Phone Number (Locked)</label>
                    <input
                      type="text"
                      value={profileData.phoneNumber}
                      disabled
                      className="w-full bg-slate-800/30 border border-slate-700/50 rounded-xl px-5 py-3.5 text-slate-500 cursor-not-allowed text-base"
                      placeholder="Not set"
                    />
                  </div>
                </div>
                <div className="pt-4">
                  <button type="submit" className="flex items-center gap-3 bg-gradient-to-r from-[#00d4ff] to-[#8b5cf6] text-white px-8 py-3.5 rounded-xl font-bold hover:opacity-90 transition-opacity text-base">
                    <Save size={20} />
                    <span>Save Profile Changes</span>
                  </button>
                </div>
              </form>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Financial Overview Card */}
              <div className="bg-slate-900 rounded-2xl p-8 border border-slate-800/80 shadow-lg shadow-black/20 flex flex-col justify-between">
                <h4 className="text-xl font-bold text-white mb-8 tracking-tight">Previous Month Summary</h4>
                <div className="flex flex-col gap-6 flex-1 justify-center">
                  <div className="flex justify-between items-center pb-6 border-b border-slate-800/50">
                    <span className="text-slate-400 text-lg">Income</span>
                    <span className="text-[#10b981] font-semibold text-xl">₹{profileData.previousMonthIncome}</span>
                  </div>
                  <div className="flex justify-between items-center pb-6 border-b border-slate-800/50">
                    <span className="text-slate-400 text-lg">Expense</span>
                    <span className="text-[#ef4444] font-semibold text-xl">₹{profileData.previousMonthExpense}</span>
                  </div>
                  <div className="flex justify-between items-center pb-6 border-b border-slate-800/50">
                    <span className="text-slate-400 text-lg">Savings</span>
                    <span className="text-[#00d4ff] font-semibold text-xl">₹{profileData.previousMonthSavings}</span>
                  </div>
                  <div className="flex justify-between items-center pb-6 border-b border-slate-800/50">
                    <span className="text-slate-400 text-lg">Savings Rate</span>
                    <span className="text-white font-semibold text-xl">{profileData.savingsPercentage}%</span>
                  </div>
                  <div className="flex flex-col gap-4 mt-2">
                    <div className="flex justify-between items-center">
                      <span className="text-slate-400 text-lg">Financial Health Score</span>
                      <span className="text-white font-bold text-xl">{profileData.financialHealthScore}<span className="text-slate-500 text-sm">/100</span></span>
                    </div>
                    <div className="w-full bg-slate-800 rounded-full h-3">
                      <div className="bg-gradient-to-r from-[#00d4ff] to-[#8b5cf6] h-3 rounded-full transition-all duration-1000 ease-out" style={{ width: `${profileData.financialHealthScore}%` }} />
                    </div>
                  </div>
                </div>
              </div>

              {/* Expense Breakdown Card */}
              <div className="bg-slate-900 rounded-2xl p-8 border border-slate-800/80 shadow-lg shadow-black/20 flex flex-col">
                <h4 className="text-xl font-bold text-white mb-8 tracking-tight">Expense Breakdown</h4>
                <div className="flex flex-col gap-5 flex-1 justify-center">
                  {Object.entries(profileData.expenseBreakdown || {}).map(([category, amount], i) => (
                    <div key={category} className="flex justify-between items-center py-2">
                      <div className="flex items-center gap-4">
                        <div className="w-4 h-4 rounded-full shadow-sm" style={{ backgroundColor: ['#00d4ff', '#8b5cf6', '#10b981', '#f97316', '#ef4444'][i % 5] }} />
                        <span className="text-slate-300 text-lg">{category}</span>
                      </div>
                      <span className="text-white font-semibold text-lg">₹{amount}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* AI Insight Card */}
              <div className="bg-slate-900 rounded-2xl p-10 border border-[#8b5cf6]/30 shadow-lg shadow-[#8b5cf6]/5 lg:col-span-2 relative overflow-hidden group">
                <div className="absolute inset-0 bg-gradient-to-r from-[#00d4ff]/10 to-[#8b5cf6]/10 opacity-50 transition-opacity duration-500" />
                <div className="relative z-10 flex flex-col gap-6">
                  <h4 className="text-xl font-bold text-white flex items-center gap-3">
                    <Cpu className="text-[#8b5cf6]" size={24} />
                    <span>AI Financial Insight</span>
                  </h4>
                  <p className="text-slate-300 leading-loose text-lg font-medium pr-8">
                    "{profileData.aiInsight}"
                  </p>
                </div>
              </div>

              {/* Financial Journey Card */}
              <div className="bg-slate-900 rounded-2xl p-8 border border-slate-800/80 shadow-lg shadow-black/20 lg:col-span-2">
                <h4 className="text-xl font-bold text-white mb-8 tracking-tight">Financial Journey Milestones</h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6 mb-10">
                  <div className="p-6 bg-slate-800/50 rounded-2xl flex flex-col justify-center">
                    <span className="block text-sm text-slate-400 mb-3 font-semibold uppercase tracking-wider">Current Streak</span>
                    <span className="block text-2xl font-bold text-white">{profileData.journeyCard.currentStreak}</span>
                  </div>
                  <div className="p-6 bg-slate-800/50 rounded-2xl flex flex-col justify-center">
                    <span className="block text-sm text-slate-400 mb-3 font-semibold uppercase tracking-wider">Best Saving Month</span>
                    <span className="block text-2xl font-bold text-[#10b981]">{profileData.journeyCard.bestSavingMonth}</span>
                  </div>
                  <div className="p-6 bg-slate-800/50 rounded-2xl flex flex-col justify-center">
                    <span className="block text-sm text-slate-400 mb-3 font-semibold uppercase tracking-wider">Highest Spending</span>
                    <span className="block text-2xl font-bold text-[#ef4444]">{profileData.journeyCard.highestSpendingCategory}</span>
                  </div>
                  <div className="p-6 bg-slate-800/50 rounded-2xl flex flex-col justify-center">
                    <span className="block text-sm text-slate-400 mb-3 font-semibold uppercase tracking-wider">Most Improved</span>
                    <span className="block text-2xl font-bold text-[#00d4ff]">{profileData.journeyCard.mostImprovedCategory}</span>
                  </div>
                </div>
                
                <div className="p-8 bg-gradient-to-r from-[#8b5cf6]/10 to-[#00d4ff]/10 rounded-2xl border border-[#8b5cf6]/20">
                  <span className="block text-sm text-[#8b5cf6] font-bold mb-4 uppercase tracking-wider">Goal Acceleration Recommendation</span>
                  <p className="text-slate-300 text-lg leading-relaxed">{profileData.journeyCard.aiRecommendation}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Security Tab */}
        {activeTab === 'security' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-2xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">Security Settings</h3>
              <p className="text-lg text-slate-400">Update your account password securely. No external OTPs required.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-10 border border-slate-800/80 shadow-lg shadow-black/20">
              <form onSubmit={handleChangePassword} className="space-y-8">
                <div className="flex flex-col gap-2">
                  <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Current Password</label>
                  <input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    required
                    className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#8b5cf6] focus:bg-slate-800 transition-colors"
                  />
                </div>
                
                <div className="flex flex-col gap-2 pt-2">
                  <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">New Password</label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#8b5cf6] focus:bg-slate-800 transition-colors"
                  />
                  <p className="text-sm text-slate-500 mt-3 font-medium">Require 8+ chars, 1 uppercase, 1 lowercase, 1 number, 1 special char.</p>
                </div>
                
                <div className="flex flex-col gap-2 pt-2">
                  <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Confirm New Password</label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#8b5cf6] focus:bg-slate-800 transition-colors"
                  />
                </div>
                
                <div className="pt-6">
                  <button type="submit" className="w-full flex justify-center items-center gap-3 bg-[#8b5cf6] text-white px-8 py-4 rounded-xl font-bold hover:bg-[#7c3aed] transition-colors text-base shadow-lg shadow-[#8b5cf6]/20">
                    <Shield size={20} />
                    <span>Update Secure Password</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* AI Preferences Tab */}
        {activeTab === 'ai' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-3xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">AI Coach Preferences</h3>
              <p className="text-lg text-slate-400">Configure how FinPilot interacts and coaches your financial decisions.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-10 border border-slate-800/80 shadow-lg shadow-black/20">
              <div className="flex flex-col gap-10">
                <div className="flex items-center justify-between pb-8 border-b border-slate-800/60">
                  <div className="pr-8">
                    <h4 className="text-lg font-bold text-white mb-2">Enable AI Financial Coach</h4>
                    <p className="text-base text-slate-400">Turn off to use the app in standard manual mode without proactive guidance.</p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer shrink-0">
                    <input type="checkbox" className="sr-only peer" checked={preferences.aiCoachEnabled} onChange={(e) => handleUpdatePreference('aiCoachEnabled', e.target.checked)} />
                    <div className="w-14 h-7 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-[#00d4ff]"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between pb-8 border-b border-slate-800/60">
                  <div className="pr-8">
                    <h4 className="text-lg font-bold text-white mb-2">Coaching Style Persona</h4>
                    <p className="text-base text-slate-400">Select the tone of voice your AI coach uses during insights.</p>
                  </div>
                  <select 
                    value={preferences.aiCoachingStyle} 
                    onChange={(e) => handleUpdatePreference('aiCoachingStyle', e.target.value)}
                    className="bg-slate-800 border border-slate-700 text-white font-medium rounded-xl px-5 py-3.5 outline-none focus:border-[#00d4ff] min-w-[200px]"
                  >
                    <option value="Friendly">Friendly & Encouraging</option>
                    <option value="Professional">Professional & Direct</option>
                    <option value="Strict Saver">Strict Saver</option>
                    <option value="Minimal Notifications">Minimal (Insights Only)</option>
                  </select>
                </div>

                {[
                  { key: 'aiDailySummary', label: 'Daily AI Digest', desc: 'Receive a short summary of your daily spending patterns.' },
                  { key: 'aiWeeklyReport', label: 'Weekly AI Report', desc: 'Comprehensive breakdown of your week with category shifts.' },
                  { key: 'aiMonthlyStory', label: 'Monthly AI Story', desc: 'A narrative recap of your monthly financial journey and habits.' },
                  { key: 'aiDecisionSimulator', label: 'AI Decision Simulator', desc: 'Enable forecasting and "What-If" purchase projections.' },
                  { key: 'aiNotifications', label: 'Real-time AI Alerts', desc: 'Get smart push notifications regarding anomalies in spending velocity.' },
                ].map((item, idx, arr) => (
                  <div key={item.key} className={`flex items-center justify-between ${idx !== arr.length - 1 ? 'pb-8 border-b border-slate-800/60' : ''}`}>
                    <div className="pr-8">
                      <h4 className="text-lg font-bold text-white mb-2">{item.label}</h4>
                      <p className="text-base text-slate-400">{item.desc}</p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer shrink-0">
                      <input type="checkbox" className="sr-only peer" checked={preferences[item.key]} onChange={(e) => handleUpdatePreference(item.key, e.target.checked)} />
                      <div className="w-14 h-7 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-[#00d4ff]"></div>
                    </label>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Financial Preferences Tab */}
        {activeTab === 'financial' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-2xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">Financial Parameters</h3>
              <p className="text-lg text-slate-400">Set up your core targets to help the AI map your trajectory.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-10 border border-slate-800/80 shadow-lg shadow-black/20 flex flex-col gap-8">
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Salary Date (Day of Month)</label>
                <input
                  type="number"
                  min="1"
                  max="31"
                  value={preferences.salaryDate}
                  onChange={(e) => handleUpdatePreference('salaryDate', parseInt(e.target.value))}
                  className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#10b981] focus:bg-slate-800 transition-colors"
                />
              </div>
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Monthly Budget Limit (₹)</label>
                <input
                  type="number"
                  value={preferences.monthlyBudget || 0}
                  onChange={(e) => handleUpdatePreference('monthlyBudget', parseFloat(e.target.value))}
                  className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#10b981] focus:bg-slate-800 transition-colors"
                />
              </div>
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Daily Soft Limit (₹)</label>
                <input
                  type="number"
                  value={preferences.dailyBudget || 0}
                  onChange={(e) => handleUpdatePreference('dailyBudget', parseFloat(e.target.value))}
                  className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#10b981] focus:bg-slate-800 transition-colors"
                />
              </div>
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Monthly Savings Target (₹)</label>
                <input
                  type="number"
                  value={preferences.savingsGoal || 0}
                  onChange={(e) => handleUpdatePreference('savingsGoal', parseFloat(e.target.value))}
                  className="w-full bg-slate-800/50 border border-slate-700 rounded-xl px-5 py-3.5 text-white text-base focus:outline-none focus:border-[#10b981] focus:bg-slate-800 transition-colors"
                />
              </div>
            </div>
          </div>
        )}

        {/* Notifications Tab */}
        {activeTab === 'notifications' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-2xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">Notification Channels</h3>
              <p className="text-lg text-slate-400">Control what alerts you receive and reduce noise.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-10 border border-slate-800/80 shadow-lg shadow-black/20">
              <div className="flex flex-col gap-8">
                {[
                  { key: 'dailyReminder', label: 'Daily Logging Reminder' },
                  { key: 'budgetAlert', label: 'Budget Threshold Alerts' },
                  { key: 'goalReminder', label: 'Goal Progress Updates' },
                  { key: 'weeklyReport', label: 'Weekly Summary Release' },
                  { key: 'monthlyReport', label: 'Monthly Summary Release' },
                  { key: 'subscriptionReminder', label: 'Upcoming Subscription Warnings' },
                  { key: 'aiFinancialTips', label: 'Ad-hoc AI Financial Insights' },
                ].map((item, idx, arr) => (
                  <div key={item.key} className={`flex items-center justify-between ${idx !== arr.length - 1 ? 'pb-8 border-b border-slate-800/50' : ''}`}>
                    <h4 className="text-lg font-medium text-white">{item.label}</h4>
                    <label className="relative inline-flex items-center cursor-pointer shrink-0">
                      <input type="checkbox" className="sr-only peer" checked={preferences[item.key]} onChange={(e) => handleUpdatePreference(item.key, e.target.checked)} />
                      <div className="w-14 h-7 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-[#f97316]"></div>
                    </label>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Appearance Tab */}
        {activeTab === 'appearance' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-2xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">Appearance</h3>
              <p className="text-lg text-slate-400">Personalize your FinPilot workspace.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-10 border border-slate-800/80 shadow-lg shadow-black/20 flex flex-col gap-8">
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Interface Theme</label>
                <select 
                  value={preferences.theme} 
                  onChange={(e) => handleUpdatePreference('theme', e.target.value)}
                  className="w-full bg-slate-800/50 border border-slate-700 text-white font-medium rounded-xl px-5 py-3.5 outline-none focus:border-[#00d4ff] transition-colors"
                >
                  <option value="Light">Light Mode</option>
                  <option value="Dark">Dark Mode (Premium)</option>
                  <option value="System">System Default</option>
                </select>
              </div>
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Accent Color Profile</label>
                <select 
                  value={preferences.accentColor} 
                  onChange={(e) => handleUpdatePreference('accentColor', e.target.value)}
                  className="w-full bg-slate-800/50 border border-slate-700 text-white font-medium rounded-xl px-5 py-3.5 outline-none focus:border-[#00d4ff] transition-colors"
                >
                  <option value="Blue">FinPilot Azure</option>
                  <option value="Purple">Deep Violet</option>
                  <option value="Emerald">Growth Emerald</option>
                </select>
              </div>
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Typography Scale</label>
                <select 
                  value={preferences.fontSize} 
                  onChange={(e) => handleUpdatePreference('fontSize', e.target.value)}
                  className="w-full bg-slate-800/50 border border-slate-700 text-white font-medium rounded-xl px-5 py-3.5 outline-none focus:border-[#00d4ff] transition-colors"
                >
                  <option value="Small">Compact</option>
                  <option value="Medium">Standard</option>
                  <option value="Large">Comfortable</option>
                </select>
              </div>
              <div className="flex flex-col gap-2">
                <label className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Localization</label>
                <select 
                  value={preferences.language} 
                  onChange={(e) => handleUpdatePreference('language', e.target.value)}
                  className="w-full bg-slate-800/50 border border-slate-700 text-white font-medium rounded-xl px-5 py-3.5 outline-none focus:border-[#00d4ff] transition-colors"
                >
                  <option value="English">English (Global)</option>
                  <option value="Hindi">Hindi (Regional)</option>
                  <option value="Spanish">Spanish</option>
                </select>
              </div>
            </div>
          </div>
        )}

        {/* Export Data Tab */}
        {activeTab === 'export' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-3xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">Data Export</h3>
              <p className="text-lg text-slate-400">Securely download your ledger history and AI reports for external processing.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-10 border border-slate-800/80 shadow-lg shadow-black/20">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <button onClick={() => handleExport('PDF')} className="group flex flex-col items-center justify-center p-8 bg-slate-800/40 rounded-2xl hover:bg-slate-800 border border-slate-700/50 hover:border-[#ef4444]/50 transition-all text-slate-300 hover:text-white shadow-sm">
                  <div className="p-4 bg-[#ef4444]/10 rounded-full mb-4 group-hover:scale-110 transition-transform">
                    <Download size={28} className="text-[#ef4444]" />
                  </div>
                  <span className="font-bold text-lg">Export PDF</span>
                  <span className="text-sm text-slate-500 mt-2 text-center">Visual Report</span>
                </button>
                <button onClick={() => handleExport('Excel')} className="group flex flex-col items-center justify-center p-8 bg-slate-800/40 rounded-2xl hover:bg-slate-800 border border-slate-700/50 hover:border-[#10b981]/50 transition-all text-slate-300 hover:text-white shadow-sm">
                  <div className="p-4 bg-[#10b981]/10 rounded-full mb-4 group-hover:scale-110 transition-transform">
                    <Download size={28} className="text-[#10b981]" />
                  </div>
                  <span className="font-bold text-lg">Export Excel</span>
                  <span className="text-sm text-slate-500 mt-2 text-center">Formatted Data</span>
                </button>
                <button onClick={() => handleExport('CSV')} className="group flex flex-col items-center justify-center p-8 bg-slate-800/40 rounded-2xl hover:bg-slate-800 border border-slate-700/50 hover:border-[#facc15]/50 transition-all text-slate-300 hover:text-white shadow-sm">
                  <div className="p-4 bg-[#facc15]/10 rounded-full mb-4 group-hover:scale-110 transition-transform">
                    <Download size={28} className="text-[#facc15]" />
                  </div>
                  <span className="font-bold text-lg">Export CSV</span>
                  <span className="text-sm text-slate-500 mt-2 text-center">Raw Ledger</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Data & Privacy Tab */}
        {activeTab === 'privacy' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-2xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">Privacy & Data Control</h3>
              <p className="text-lg text-slate-400">Total ownership over your footprint and AI context memory.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-10 border border-slate-800/80 shadow-lg shadow-black/20 flex flex-col gap-6">
              <button onClick={() => showNotification('Data archive requested. You will receive an email shortly.', 'info')} className="w-full flex items-center justify-between p-6 bg-slate-800/40 hover:bg-slate-800 border border-slate-700/50 hover:border-slate-600 rounded-2xl text-left transition-all group">
                <div className="flex flex-col gap-1">
                  <span className="block text-white font-bold text-lg">Download Data Archive</span>
                  <span className="text-sm text-slate-400">Get a ZIP copy of all your raw data securely.</span>
                </div>
                <div className="p-3 bg-slate-700/50 rounded-xl group-hover:bg-slate-700 transition-colors">
                  <Download size={20} className="text-slate-300" />
                </div>
              </button>
              
              <button onClick={() => showNotification('AI Memory cleared.', 'success')} className="w-full flex items-center justify-between p-6 bg-slate-800/40 hover:bg-slate-800 border border-slate-700/50 hover:border-slate-600 rounded-2xl text-left transition-all group">
                <div className="flex flex-col gap-1">
                  <span className="block text-white font-bold text-lg">Clear AI Context Memory</span>
                  <span className="text-sm text-slate-400">Force the AI to forget your habits and re-learn.</span>
                </div>
                <div className="p-3 bg-slate-700/50 rounded-xl group-hover:bg-slate-700 transition-colors">
                  <Cpu size={20} className="text-slate-300" />
                </div>
              </button>
              
              <button onClick={() => showNotification('Ledger cleared.', 'success')} className="w-full flex items-center justify-between p-6 bg-slate-800/40 hover:bg-slate-800 border border-slate-700/50 hover:border-slate-600 rounded-2xl text-left transition-all group">
                <div className="flex flex-col gap-1">
                  <span className="block text-white font-bold text-lg">Wipe Expense Ledger</span>
                  <span className="text-sm text-slate-400">Delete all transactions, keeping account settings active.</span>
                </div>
                <div className="p-3 bg-slate-700/50 rounded-xl group-hover:bg-slate-700 transition-colors">
                  <Database size={20} className="text-slate-300" />
                </div>
              </button>
              
              <div className="mt-10 pt-10 border-t border-red-900/30">
                <h4 className="text-red-400 font-bold text-lg mb-4 uppercase tracking-wider">Danger Zone</h4>
                <button onClick={handleDeleteAccount} className="w-full flex justify-center items-center gap-3 bg-red-500/10 text-red-500 border border-red-500/20 px-8 py-5 rounded-2xl font-bold hover:bg-red-500 hover:text-white transition-all text-lg">
                  <span>Delete Entire Account Permanently</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* About Tab */}
        {activeTab === 'about' && (
          <div className="space-y-12 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-2xl">
            <div className="mb-12">
              <h3 className="text-3xl font-bold text-white mb-3 tracking-tight">About FinPilot</h3>
              <p className="text-lg text-slate-400">Platform versions and legal information.</p>
            </div>
            
            <div className="bg-slate-900 rounded-2xl p-12 border border-slate-800/80 shadow-lg shadow-black/20 flex flex-col items-center text-center">
              <div className="flex flex-col items-center justify-center pb-10 border-b border-slate-800/50 w-full mb-10">
                <div className="w-24 h-24 bg-gradient-to-tr from-[#00d4ff] to-[#8b5cf6] rounded-3xl flex items-center justify-center shadow-2xl shadow-[#8b5cf6]/30 mb-6">
                  <span className="text-white font-black text-4xl">FP</span>
                </div>
                <h4 className="text-3xl font-bold text-white mb-2 tracking-tight">FinPilot AI</h4>
                <p className="text-lg text-[#00d4ff] font-medium">Version 2.4.0-premium</p>
              </div>
              
              <div className="flex flex-col gap-6 w-full max-w-xs">
                <a href="#" className="p-4 bg-slate-800/30 hover:bg-slate-800 rounded-xl text-slate-300 hover:text-[#00d4ff] font-semibold transition-colors border border-transparent hover:border-[#00d4ff]/30">Privacy Policy</a>
                <a href="#" className="p-4 bg-slate-800/30 hover:bg-slate-800 rounded-xl text-slate-300 hover:text-[#00d4ff] font-semibold transition-colors border border-transparent hover:border-[#00d4ff]/30">Terms & Conditions</a>
                <a href="#" className="p-4 bg-slate-800/30 hover:bg-slate-800 rounded-xl text-slate-300 hover:text-[#00d4ff] font-semibold transition-colors border border-transparent hover:border-[#00d4ff]/30">Developer Contact & Support</a>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
