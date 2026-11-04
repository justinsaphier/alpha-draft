import { useState } from 'react';

// Keeps every field of the stock form in one state object, plus the error for
// each field. One change handler serves every input and clears that field's
// error as soon as the user starts fixing it.
function useStockFormFields(initialValues) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});

  function handleChange(field) {
    return (e) => {
      const { value } = e.target;
      setValues((current) => ({ ...current, [field]: value }));
      setErrors((current) => ({ ...current, [field]: undefined }));
    };
  }

  function reset(nextValues = initialValues) {
    setValues(nextValues);
    setErrors({});
  }

  return { values, errors, setErrors, handleChange, reset };
}

export default useStockFormFields;