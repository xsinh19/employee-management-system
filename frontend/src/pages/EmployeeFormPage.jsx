import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { employeeApi } from '../api/employeeApi';
import { getErrorMessage } from '../api/axiosClient';

const EMPTY = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  department: '',
  designation: '',
  salary: '',
  dateOfJoining: '',
};

const FIELDS = [
  { name: 'firstName', label: 'First name', required: true },
  { name: 'lastName', label: 'Last name', required: true },
  { name: 'email', label: 'Email', type: 'email', required: true },
  { name: 'phone', label: 'Phone' },
  { name: 'department', label: 'Department', required: true, list: 'department-options' },
  { name: 'designation', label: 'Designation', required: true },
  { name: 'salary', label: 'Annual salary (INR)', type: 'number', required: true, min: 1, step: '0.01' },
  { name: 'dateOfJoining', label: 'Date of joining', type: 'date', required: true },
];

export default function EmployeeFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY);
  const [departments, setDepartments] = useState([]);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    employeeApi.departments().then(setDepartments).catch(() => {});
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    employeeApi
      .get(id)
      .then((e) => {
        const loaded = {};
        Object.keys(EMPTY).forEach((k) => (loaded[k] = e[k] ?? ''));
        setForm(loaded);
      })
      .catch((err) => setError(getErrorMessage(err, 'Could not load employee')))
      .finally(() => setLoading(false));
  }, [id, isEdit]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
    setFieldErrors((fe) => ({ ...fe, [name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setFieldErrors({});
    const payload = { ...form, salary: form.salary === '' ? null : Number(form.salary) };
    try {
      const saved = isEdit ? await employeeApi.update(id, payload) : await employeeApi.create(payload);
      const verb = isEdit ? 'Updated' : 'Added';
      navigate('/employees', { state: { flash: `${verb} ${saved.firstName} ${saved.lastName}` } });
    } catch (err) {
      // 400 responses carry per-field messages from Bean Validation
      if (err.response?.data?.fieldErrors) setFieldErrors(err.response.data.fieldErrors);
      setError(getErrorMessage(err, 'Save failed'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <main className="container">Loading…</main>;

  return (
    <main className="container narrow">
      <div className="page-header">
        <h2>{isEdit ? 'Edit employee' : 'Add employee'}</h2>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <form className="card form-grid" onSubmit={handleSubmit} noValidate>
        {FIELDS.map((f) => (
          <label key={f.name} className={`field ${fieldErrors[f.name] ? 'has-error' : ''}`}>
            <span>
              {f.label}
              {f.required && <em className="req">*</em>}
            </span>
            <input
              name={f.name}
              type={f.type || 'text'}
              value={form[f.name]}
              onChange={handleChange}
              list={f.list}
              min={f.min}
              step={f.step}
              max={f.type === 'date' ? new Date().toISOString().slice(0, 10) : undefined}
            />
            {fieldErrors[f.name] && <small className="field-error">{fieldErrors[f.name]}</small>}
          </label>
        ))}

        <datalist id="department-options">
          {departments.map((d) => (
            <option key={d} value={d} />
          ))}
        </datalist>

        <div className="form-actions">
          <Link to="/employees" className="btn btn-ghost">
            Cancel
          </Link>
          <button className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create employee'}
          </button>
        </div>
      </form>
    </main>
  );
}
