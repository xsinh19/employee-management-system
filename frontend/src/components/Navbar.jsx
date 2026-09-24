import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <Link to="/employees" className="brand">
        EMS
      </Link>
      <div className="navbar-right">
        <span className="user-chip">
          {user.username}
          <span className={`role-badge role-${user.role.toLowerCase()}`}>{user.role}</span>
        </span>
        <button className="btn btn-ghost" onClick={handleLogout}>
          Log out
        </button>
      </div>
    </header>
  );
}
