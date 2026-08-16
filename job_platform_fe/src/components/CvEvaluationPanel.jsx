import { useState, useRef } from 'react';
import api from '../service/api';

/**
 * CvEvaluationPanel
 * Props:
 *  - onResult(resultText): callback khi có kết quả đánh giá → widget sẽ push vào messages
 *  - onBack(): callback quay về menu
 *  - hasProfileCv: boolean — user có CV trong hồ sơ chưa
 *  - profileCvUrl: string — đường dẫn CV profile (để hiển thị tên file)
 */
function CvEvaluationPanel({ onResult, onBack, hasProfileCv, profileCvUrl }) {
  const [cvSource, setCvSource] = useState(hasProfileCv ? 'profile' : 'upload'); // 'profile' | 'upload'
  const [uploadedFile, setUploadedFile] = useState(null);
  const [jobCode, setJobCode] = useState('');
  const [jobCodeError, setJobCodeError] = useState('');
  const [loading, setLoading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);
  

  const profileCvName = profileCvUrl
    ? profileCvUrl.split('/').pop()
    : 'CV của bạn';

  const handleFile = (file) => {
    if (!file) return;
    const allowed = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!allowed.includes(file.type)) {
      alert('Chỉ hỗ trợ file PDF hoặc DOCX!');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      alert('File tối đa 5MB!');
      return;
    }
    setUploadedFile(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    handleFile(e.dataTransfer.files[0]);
  };

  const handleJobCodeChange = (e) => {
    let val = e.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, '');
    // Auto-prefix JP- nếu user chưa gõ
    if (val && !val.startsWith('JP-')) {
      if (val.startsWith('JP')) val = 'JP-' + val.slice(2);
      else val = 'JP-' + val;
    }
    if (val.length > 9) val = val.slice(0, 9); // JP- + 6 ký tự = 9
    setJobCode(val);
    setJobCodeError('');
  };

  const validate = () => {
    if (cvSource === 'upload' && !uploadedFile) {
      alert('Vui lòng upload CV của bạn!');
      return false;
    }
    const codeRegex = /^JP-[A-Z0-9]{6}$/;
    if (!codeRegex.test(jobCode)) {
      setJobCodeError('Mã bài đăng không hợp lệ. Ví dụ: JP-AB12CD');
      return false;
    }
    return true;
  };

  const handleEvaluate = async () => {
    if (!validate()) return;
    setLoading(true);

    try {
      let res;

      if (cvSource === 'profile') {
        // Dùng CV profile sẵn có
        res = await api.post('/ai/evaluate-cv', {
          jobCode,
          useProfileCv: true,
        });
      } else {
        // Upload file
        const formData = new FormData();
        formData.append('cv', uploadedFile);
        formData.append('jobCode', jobCode);
        res = await api.post('/ai/evaluate-cv-file', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
      }

      if (res.data.success) {
        const formattedText = res.data.data.split('\n').map((line, index) => (
          <span key={index}>
            {line}
            <br />
          </span>
        ));
        onResult(formattedText);
      } else {
        onResult('Đã có lỗi xảy ra khi đánh giá. Vui lòng thử lại!');
      }
    } catch (err) {
        console.log(err);
        console.log(err.response);
        console.log(err.response?.data);

        onResult(
            err.response?.data?.message ||
            err.response?.data?.error ||
            err.message
        );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '20px' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        <button
          onClick={onBack}
          style={{
            background: 'none', border: 'none', cursor: 'pointer',
            fontSize: '18px', color: '#666', padding: '0 4px', lineHeight: 1,
          }}
          title="Quay lại"
        >
          ←
        </button>
        <div>
          <div style={{ fontWeight: '700', fontSize: '15px', color: '#222' }}>📄 Đánh giá CV</div>
          <div style={{ fontSize: '12px', color: '#888' }}>So sánh CV với bài tuyển dụng</div>
        </div>
      </div>

      {/* ── Bước 1: Chọn nguồn CV ── */}
      <div>
        <label style={labelStyle}>Bước 1 — CV của bạn</label>

        <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
          {hasProfileCv && (
            <TabBtn active={cvSource === 'profile'} onClick={() => setCvSource('profile')}>
              Hồ sơ cá nhân
            </TabBtn>
          )}
          <TabBtn active={cvSource === 'upload'} onClick={() => setCvSource('upload')}>
            Upload CV mới
          </TabBtn>
        </div>

        {cvSource === 'profile' && hasProfileCv && (
          <div style={profileCvBox}>
            <span style={{ fontSize: '22px' }}>📋</span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: '13px', fontWeight: '600', color: '#222', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {profileCvName}
              </div>
              <div style={{ fontSize: '11px', color: '#888' }}>CV trong hồ sơ của bạn</div>
            </div>
            <span style={{ fontSize: '18px' }}>✅</span>
          </div>
        )}

        {cvSource === 'upload' && (
          <div
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            style={{
              ...dropzone,
              borderColor: dragOver ? 'var(--primary-color, #6366f1)' : uploadedFile ? '#22c55e' : '#d1d5db',
              background: dragOver ? '#f5f3ff' : uploadedFile ? '#f0fdf4' : '#fafafa',
            }}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".pdf,.docx"
              style={{ display: 'none' }}
              onChange={(e) => handleFile(e.target.files[0])}
            />
            {uploadedFile ? (
              <>
                <span style={{ fontSize: '28px' }}>✅</span>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '13px', fontWeight: '600', color: '#16a34a' }}>{uploadedFile.name}</div>
                  <div style={{ fontSize: '11px', color: '#888' }}>{(uploadedFile.size / 1024).toFixed(0)} KB</div>
                </div>
                <button
                  onClick={(e) => { e.stopPropagation(); setUploadedFile(null); }}
                  style={{ fontSize: '11px', color: '#ef4444', background: 'none', border: 'none', cursor: 'pointer' }}
                >
                  Xóa và chọn lại
                </button>
              </>
            ) : (
              <>
                <span style={{ fontSize: '32px' }}>📂</span>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '13px', fontWeight: '600', color: '#444' }}>Kéo thả hoặc bấm để chọn file</div>
                  <div style={{ fontSize: '11px', color: '#999', marginTop: '4px' }}>PDF hoặc DOCX · Tối đa 5MB</div>
                </div>
              </>
            )}
          </div>
        )}
      </div>

      {/* ── Bước 2: Nhập mã bài đăng ── */}
      <div>
        <label style={labelStyle}>Bước 2 — Mã bài tuyển dụng</label>
        <input
          type="text"
          placeholder="JP-XXXXXX"
          value={jobCode}
          onChange={handleJobCodeChange}
          maxLength={9}
          style={{
            ...inputStyle,
            borderColor: jobCodeError ? '#ef4444' : '#d1d5db',
            letterSpacing: '2px',
            fontWeight: '600',
            fontSize: '15px',
          }}
        />
        {jobCodeError && (
          <div style={{ fontSize: '12px', color: '#ef4444', marginTop: '4px' }}>{jobCodeError}</div>
        )}
        <div style={{ fontSize: '11px', color: '#999', marginTop: '4px' }}>
          Mã hiển thị trên trang bài tuyển dụng (VD: JP-AB12CD)
        </div>
      </div>

      {/* ── Nút đánh giá ── */}
      <button
        onClick={handleEvaluate}
        disabled={loading}
        style={{
          padding: '13px',
          background: loading ? '#a5b4fc' : 'var(--primary-color, #6366f1)',
          color: 'white',
          border: 'none',
          borderRadius: '10px',
          fontSize: '14px',
          fontWeight: '700',
          cursor: loading ? 'not-allowed' : 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '8px',
          transition: 'background 0.2s',
        }}
      >
        {loading ? (
          <>
            <span style={{ display: 'inline-block', animation: 'spin 1s linear infinite' }}>⏳</span>
            Đang phân tích...
          </>
        ) : (
          '🔍 Đánh giá độ phù hợp'
        )}
      </button>

    </div>
  );
}

// ── Shared micro-components ──

function TabBtn({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      style={{
        flex: 1,
        padding: '8px 12px',
        borderRadius: '8px',
        border: `1.5px solid ${active ? 'var(--primary-color, #6366f1)' : '#e0e0e0'}`,
        background: active ? 'var(--primary-color, #6366f1)' : 'white',
        color: active ? 'white' : '#555',
        fontSize: '13px',
        fontWeight: active ? '600' : '400',
        cursor: 'pointer',
        transition: 'all 0.15s',
      }}
    >
      {children}
    </button>
  );
}

// ── Styles ──
const labelStyle = {
  display: 'block',
  fontSize: '12px',
  fontWeight: '700',
  color: '#555',
  textTransform: 'uppercase',
  letterSpacing: '0.5px',
  marginBottom: '8px',
};

const profileCvBox = {
  display: 'flex',
  alignItems: 'center',
  gap: '12px',
  padding: '12px 14px',
  background: '#f0fdf4',
  border: '1.5px solid #86efac',
  borderRadius: '10px',
};

const dropzone = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  gap: '10px',
  padding: '28px 16px',
  border: '2px dashed',
  borderRadius: '10px',
  cursor: 'pointer',
  transition: 'all 0.2s',
};

const inputStyle = {
  width: '100%',
  padding: '11px 14px',
  border: '1.5px solid',
  borderRadius: '8px',
  fontSize: '15px',
  outline: 'none',
  boxSizing: 'border-box',
  fontFamily: 'monospace',
};

export default CvEvaluationPanel;