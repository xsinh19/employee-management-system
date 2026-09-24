import axiosClient from './axiosClient';

export const authApi = {
  login: (username, password) =>
    axiosClient.post('/auth/login', { username, password }).then((r) => r.data),
};

export const employeeApi = {
  list: (params) => axiosClient.get('/employees', { params }).then((r) => r.data),
  get: (id) => axiosClient.get(`/employees/${id}`).then((r) => r.data),
  departments: () => axiosClient.get('/employees/departments').then((r) => r.data),
  create: (employee) => axiosClient.post('/employees', employee).then((r) => r.data),
  update: (id, employee) => axiosClient.put(`/employees/${id}`, employee).then((r) => r.data),
  remove: (id) => axiosClient.delete(`/employees/${id}`),
};
