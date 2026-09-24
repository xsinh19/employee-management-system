import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { employeeApi } from '../api/employeeApi';
import { getErrorMessage } from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import ConfirmDialog from '../components/ConfirmDialog';

const PAGE_SIZE = 10;
const COLUMNS = [
  { key: 'firstName', label: 'Name' },
  { key: 'email', label: 'Email' },
  { key: 'department', label: 'Department' },
  { key: 'designation', label: 'Designation' },
  { key: 'salary', label: 'Salary' },
  { key: 'dateOfJoining', label: 'Joined' },
];

const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export default function EmployeeListPage() {
  const { isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [department, setDepartment] = useState('');
  const [departments, setDepartments] = useState([]);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState({ by: 'id', direction: 'asc' });
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [flash, setFlash] = useState(location.state?.flash || '');
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  // Clear the one-time flash message from history so a refresh doesn't show it again
  useEffect(() => {
    if (location.state?.flash) navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate]);

  // Debounce the search box so we don't send a request on every keystroke
  useEffect(() => {
    const t = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  useEffect(() => {
    employeeApi.departments().then(setDepartments).catch(() => setDepartments([]));
  }, [reloadKey]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    employeeApi
      .list({
        search: search || undefined,
        department: department || undefined,
        page,
        size: PAGE_SIZE,
        sortBy: sort.by,
        direction: sort.direction,
      })
      .then((result) => {
        if (cancelled) return;
        // If we deleted the last row on the last page, step back one page
        if (result.content.length === 0 && result.page > 0) {
          setPage(result.page - 1);
          return;
        }
        setData(result);
        setError('');
      })
      .catch((err) => !cancelled && setError(getErrorMessage(err, 'Failed to load employees')))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true; // ignore responses from outdated requests
    };
  }, [search, department, page, sort, reloadKey]);

  const toggleSort = (key) => {
    setSort((s) => ({ by: key, direction: s.by === key && s.direction === 'asc' ? 'desc' : 'asc' }));
    setPage(0);
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await employeeApi.remove(toDelete.id);
      setFlash(`Deleted ${toDelete.firstName} ${toDelete.lastName}`);
      setToDelete(null);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setError(getErrorMessage(err, 'Delete failed'));
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  const sortIndicator = (key) => (sort.by === key ? (sort.direction === 'asc' ? ' ▲' : ' ▼') : '');

  return (
    <main className="container">
      <div className="page-header">
        <div>
          <h2>Employees</h2>
          <p className="muted">{data ? `${data.totalElements} total` : ' '}</p>
        </div>
        {isAdmin && (
          <Link to="/employees/new" className="btn btn-primary">
            + Add employee
          </Link>
        )}
      </div>

      {flash && (
        <div className="alert alert-success" onClick={() => setFlash('')}>
          {flash}
        </div>
      )}
      {error && <div className="alert alert-error">{error}</div>}

      <div className="toolbar">
        <input
          className="search"
          placeholder="Search name, email or designation…"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
        <select
          value={department}
          onChange={(e) => {
            setDepartment(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All departments</option>
          {departments.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
      </div>

      <div className="card table-card">
        <table>
          <thead>
            <tr>
              {COLUMNS.map((c) => (
                <th key={c.key} onClick={() => toggleSort(c.key)} className="sortable">
                  {c.label}
                  {sortIndicator(c.key)}
                </th>
              ))}
              {isAdmin && <th className="actions-col">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {data?.content.map((e) => (
              <tr key={e.id}>
                <td>
                  {e.firstName} {e.lastName}
                </td>
                <td>{e.email}</td>
                <td>
                  <span className="pill">{e.department}</span>
                </td>
                <td>{e.designation}</td>
                <td className="num">{currency.format(e.salary)}</td>
                <td>{e.dateOfJoining}</td>
                {isAdmin && (
                  <td className="actions-col">
                    <Link to={`/employees/${e.id}/edit`} className="btn btn-small">
                      Edit
                    </Link>
                    <button className="btn btn-small btn-danger-outline" onClick={() => setToDelete(e)}>
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={COLUMNS.length + (isAdmin ? 1 : 0)} className="empty">
                  No employees match your filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        {loading && <div className="loading-bar" />}
      </div>

      {data && data.totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>
            ← Prev
          </button>
          <span>
            Page {data.page + 1} of {data.totalPages}
          </span>
          <button
            className="btn btn-ghost"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage(page + 1)}
          >
            Next →
          </button>
        </div>
      )}

      {toDelete && (
        <ConfirmDialog
          title="Delete employee?"
          message={`${toDelete.firstName} ${toDelete.lastName} will be permanently removed.`}
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setToDelete(null)}
        />
      )}
    </main>
  );
}
