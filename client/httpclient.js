const http = require('axios');
const jwtDecode = require('jwt-decode');
const _ = require('lodash');

const getToken = () => {
  const token = localStorage.token;
  if (_.isNil(token)) return null;
  const decoded = jwtDecode(token);
  const exp = new Date(decoded.exp*1000);
  const now = new Date();
  if ( now >= exp ) return null;
  return token;
};

const defaultOptions = {
  headers: {
    Authorization: getToken() ? `Bearer ${localStorage.token}` : ''
  }
};

module.exports = {
  get: (url, options = {}) => axios.get(url, { ...defaultOptions, ...options }),
  post: (url, data, options = {}) => axios.post(url, data, { ...defaultOptions, ...options })
};