import { useState } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getErrorMessage } from '../api/axiosClient';

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState(params.get('expired') ? 'Your session expired. Please log in again.' : '');
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to="/employees" replace />;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await login(form.username.trim(), form.password);
      navigate('/employees', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'Login failed'));
    } finally {
      setSubmitting(false);
    }
  };

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={handleSubmit}>
        <h1>Employee Management</h1>
        <p className="muted">Sign in to continue</p>

        {error && <div className="alert alert-error">{error}</div>}

        <label className="field">
          <span>Username</span>
          <input value={form.username} onChange={update('username')} autoComplete="username" required autoFocus />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            value={form.password}
            onChange={update('password')}
            autoComplete="current-password"
            required
          />
        </label>

        <button className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>

        <div className="demo-creds">
          <strong>Demo accounts</strong>
          <span>admin / admin123 (full access)</span>
          <span>user / user123 (read-only)</span>
        </div>
      </form>
    </div>
  );
}
