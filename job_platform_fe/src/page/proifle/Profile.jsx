import { useEffect, useState, useRef  } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { checkToken } from '../../service/api';
import './profile.css';
import VipHistoryModal from '../../components/VipHistoryModal';

function EditableField({ label, value, onSave, icon = "✏️" }) {
  const [isEditing, setIsEditing] = useState(false);
  const [inputValue, setInputValue] = useState(value || '');
  const [prevValue, setPrevValue] = useState(value);

  if (value !== prevValue) {
    setPrevValue(value);
    setInputValue(value || '');
  }

  const handleBlurOrEnter = () => {
    setIsEditing(false);
    if (inputValue !== value) onSave(inputValue);
  };

  return (
    <div className="profile-field-modern editable">
      <span className="field-label-modern">{icon} {label}:</span>
      {isEditing ? (
        <input
          type="text"
          className="field-input-modern"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onBlur={handleBlurOrEnter}
          onKeyDown={(e) => e.key === 'Enter' && handleBlurOrEnter()}
          autoFocus
        />
      ) : (
        <div className={`field-value-display editable-text ${value ? '' : 'field-empty'}`} onClick={() => setIsEditing(true)}>
          {value || `(Bấm vào đây để bổ sung ${label.toLowerCase()})`}
        </div>
      )}
    </div>
  );
}

function ReadOnlyField({ label, value, icon = "📌" }) {
  return (
    <div className="profile-field-modern">
      <span className="field-label-modern">{icon} {label}:</span>
      <div className={`field-value-display ${value ? '' : 'field-empty'}`}>
        {value || '(Chưa có dữ liệu)'}
      </div>
    </div>
  );
}

function Profile() {
  const [profileData, setProfileData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notify, setNotify] = useState({ type: '', msg: '' });
  const [aiLoadingField, setAiLoadingField] = useState('');
  const [showCvPreview, setShowCvPreview] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [passWordData, setPassWordData] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [passWordLoading, setPassWordLoading] = useState(false);

  // State for upgrading to VIP
  const [upgrading, setUpgrading] = useState(false);
  
  const [cvFile, setCvFile] = useState(null);
  const [uploadingCv, setUploadingCv] = useState(false);
  const [cvBlobUrl, setCvBlobUrl] = useState('');
  const [cvBlobLoading, setCvBlobLoading] = useState(false);

  // State for uploading company logo
  const [logoFile, setLogoFile] = useState(null);
  const [uploadingLogo, setUploadingLogo] = useState(false);
  const logoInputRef = useRef(null);

  const navigate = useNavigate();

  useEffect(() => {
    const fetchProfile = async () => {
      const user = await checkToken();
      if (!user) { navigate('/login'); return; }
      try {
        const res = await api.get(`/profile/${user.id}/${user.role}`);
        setProfileData(res.data);
        setLoading(false);
      } catch {
        setNotify({ type: 'error', msg: 'Lỗi tải hồ sơ!' });
        setLoading(false);
      }
    };
    fetchProfile();
  }, [navigate]);

  useEffect(() => {
    return () => {
      if (cvBlobUrl) URL.revokeObjectURL(cvBlobUrl);
    };
  }, [cvBlobUrl]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentStatus = params.get('payment');
    if (paymentStatus === 'success') {
      setNotify({ type: 'success', msg: '🎉 Nâng cấp VIP thành công!' });
      window.history.replaceState({}, '', '/profile');
    } else if (paymentStatus === 'failed') {
      setNotify({ type: 'error', msg: '❌ Thanh toán không thành công, vui lòng thử lại!' });
      window.history.replaceState({}, '', '/profile');
    }
  }, []);

  const handleUpdateField = async (fieldName, updatedValue) => {
    const updatedData = { ...profileData, [fieldName]: updatedValue };
    try {
      const user = await checkToken();
      if (!user) { navigate('/login'); return; }
      await api.put(`/profile/${user.id}/${profileData.role}`, updatedData);
      setNotify({ type: 'success', msg: 'Đã lưu thay đổi!' });
      setTimeout(() => setNotify({ type: '', msg: '' }), 2000);
      const reload = await api.get(`/profile/${user.id}/${profileData.role}`);
      setProfileData(reload.data);
    } catch {
      setNotify({ type: 'error', msg: 'Lỗi lưu dữ liệu!' });
    }
  };

  // Function to handle upgrading to VIP
  const handleUpgradeVip = async (days) => {
    setUpgrading(true);
    try {
      const res = await api.post('/payment/vnpay/create', { days });
      window.location.href = res.data.data.paymentUrl;
    } catch (err) {
      setNotify({ type: 'error', msg: err.response?.data?.message || 'Không thể tạo link thanh toán!' });
      setUpgrading(false);
    }
  };

  // Function to get the full URL for the company logo
  const getLogoUrl = (logoPath) => {
    if (!logoPath) return null;
    if (logoPath.startsWith('http')) return logoPath;
    if (logoPath.startsWith('/images/')) return logoPath;
    const baseUrl = import.meta.env.VITE_API_BASE_URL.replace('/api/v1', '');
    return `${baseUrl}${logoPath}`;
  };
  // Function to handle company logo upload
  const handleLogoChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const allowedExt = ['.jpg', '.jpeg', '.png'];
    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    if (!allowedExt.includes(ext)) {
      setNotify({ type: 'error', msg: 'Chỉ chấp nhận file JPG hoặc PNG!' });
      e.target.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setNotify({ type: 'error', msg: 'File tối đa 2MB!' });
      e.target.value = '';
      return;
    }

    setUploadingLogo(true);
    try {
      const user = await checkToken();
      if (!user) { navigate('/login'); return; }

      const formData = new FormData();
      formData.append('file', file);

      const res = await api.post(`/profile/${user.id}/upload-logo`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      setProfileData(res.data);
      setNotify({ type: 'success', msg: 'Đã cập nhật logo công ty!' });
      setTimeout(() => setNotify({ type: '', msg: '' }), 2000);
    } catch (err) {
      setNotify({ type: 'error', msg: err.response?.data || 'Lỗi tải logo lên!' });
      console.error(err);
    } finally {
      setUploadingLogo(false);
      e.target.value = '';
    }
  };
  
  const handleCvChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const allowedExt = ['.pdf', '.doc', '.docx'];
    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    if (!allowedExt.includes(ext)) {
      setNotify({ type: 'error', msg: 'Chỉ chấp nhận file PDF hoặc Word (.doc, .docx)!' });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setNotify({ type: 'error', msg: 'File tối đa 5MB!' });
      return;
    }
    setCvFile(file);
  };

  

  const handleUploadCv = async () => {
    if (!cvFile) {
      setNotify({ type: 'error', msg: 'Chưa chọn file CV!' });
      return;
    }
    setUploadingCv(true);
    try {
      const user = await checkToken();
      if (!user) { navigate('/login'); return; }

      const formData = new FormData();
      formData.append('file', cvFile);

      const res = await api.post(`/profile/${user.id}/upload-cv`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      setProfileData(res.data);
      setNotify({ type: 'success', msg: 'Tải CV lên thành công!' });
      setCvFile(null);

      if (cvBlobUrl) {
        URL.revokeObjectURL(cvBlobUrl);
        setCvBlobUrl('');
      }
    } catch (err) {
      setNotify({ type: 'error', msg: 'Lỗi tải CV lên!' });
      console.error(err);
    } finally {
      setUploadingCv(false);
    }
  };

  const fetchCvBlob = async () => {
    if (!profileData?.cvFileName) return null;
    const user = await checkToken();
    if (!user) { navigate('/login'); return null; }

    try {
      const res = await api.get(`/files/cv/${profileData.cvFileName}`, {
        responseType: 'blob',
      });
      return res.data;
    } catch (err) {
      setNotify({ type: 'error', msg: 'Không thể tải CV!' });
      console.error(err);
      return null;
    }
  };

  const openCvPreview = async () => {
    setCvBlobLoading(true);
    const blob = await fetchCvBlob();
    setCvBlobLoading(false);
    if (!blob) return;

    const pdfBlob = new Blob([blob], { type: 'application/pdf' });
    if (cvBlobUrl) URL.revokeObjectURL(cvBlobUrl);
    const url = URL.createObjectURL(pdfBlob);
    setCvBlobUrl(url);
    setShowCvPreview(true);
  };

  const closeCvPreview = () => {
    if (cvBlobUrl) URL.revokeObjectURL(cvBlobUrl);
    setCvBlobUrl('');
    setShowCvPreview(false);
  };

  const downloadCvWord = async () => {
    setCvBlobLoading(true);
    const blob = await fetchCvBlob();
    setCvBlobLoading(false);
    if (!blob) return;

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = profileData.cvFileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  };

  // Function to handle verification of a single field using AI
  const handleVerifySingleField = async (fieldType, value) => {
    if (!value?.trim()) {
      alert("Điền dữ liệu vào ô trước khi quét!");
      return;
    }
    setAiLoadingField(fieldType);
    setNotify({ type: 'info', msg: 'Đang xác thực với AI...' });

    try {
      const user = await checkToken();
      if (!user) { navigate('/login'); return; }

      const updatedData = { ...profileData, [fieldType]: value };
      await api.put(`/profile/${user.id}/${profileData.role}`, updatedData);

      const response = await api.post(`/profile/verify-field/${user.id}`, {
        fieldType: fieldType,
        value: value
      });

      const reason = response.data?.reason || response.data?.message || '';

      // 1. ƯU TIÊN BẮT LỖI CÀO WEB / CHỜ ADMIN DUYỆT TAY TRƯỚC
      if (reason.includes("chặn truy cập") || reason.includes("chờ admin") || reason.includes("thủ công") || reason.includes("Chờ admin")) {
        const reload = await api.get(`/profile/${user.id}/${profileData.role}`);
        setProfileData(reload.data);
        setNotify({ 
          type: 'info', 
          msg: `ℹ️ Website chặn bot cào dữ liệu tự động. Hệ thống đã lưu yêu cầu, vui lòng chờ Admin kiểm duyệt thủ công!` 
        });
      } 
      // 2. NẾU KHÔNG BỊ WAF CHẶN THÌ MỚI ĐÁNH GIÁ THÀNH CÔNG THẬT SỰ
      else if (response.data && response.data.status === "SUCCESS") {
        const reload = await api.get(`/profile/${user.id}/${profileData.role}`);
        setProfileData(reload.data);
        setNotify({ type: 'success', msg: `✅ Duyệt thành công!` });
      } 
      // 3. TỪ CHỐI DO DỮ LIỆU SAI LỆCH
      else {
        setNotify({ type: 'error', msg: `❌ Từ chối: ${reason || 'Dữ liệu không khớp'}` });
      }
    } catch (err) {
      setNotify({ type: 'error', msg: 'Lỗi hệ thống!' });
      console.error(err);
    } finally {
      setAiLoadingField('');
    }
  };

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    if (!passWordData.oldPassword || !passWordData.newPassword || !passWordData.confirmPassword) {
      setNotify({ type: 'error', msg: 'Vui lòng điền đầy đủ thông tin!' });
      return;
    }
    if(passWordData.newPassword !== passWordData.confirmPassword) {
      setNotify({ type: 'error', msg: 'Mật khẩu mới và xác nhận không khớp!' });
      return;
    }
    if(passWordData.newPassword.length < 6) {
      setNotify({ type: 'error', msg: 'Mật khẩu mới phải từ 6 ký tự!' });
      return;
    }
    setPassWordLoading(true);
    try{
      const user = await checkToken();
      if (!user) { navigate('/login'); return; }

      await api.post(`/profile/change-password/${user.id}`, {
        oldPassword: passWordData.oldPassword, 
        newPassword: passWordData.newPassword
      });
      setPassWordData({oldPassword: '', newPassword: '', confirmPassword: ''});
      setShowPasswordModal(false);
      setNotify({ type: 'success', msg: 'Đổi mật khẩu thành công!' });
      setTimeout(() => setNotify({ type: '', msg: '' }), 2000);
    }catch(err){
      const errMsg = err.response?.data?.message || 'Lỗi đổi mật khẩu!';
      setNotify({ type: 'error', msg: errMsg });
    }finally{
      setPassWordLoading(false);
    }
  };

  const renderStatusBadge = (status) => {
    // Chưa xác thực gì
    if (!status || status === "pending") {
      return {
        class: "status-default",
        text: "Chưa xác thực (60đ)",
      };
    }

    const parts = [];

    if (status.includes("name")) {
      parts.push("Tên");
    }

    if (status.includes("tax")) {
      parts.push("Thuế");
    }

    const hasWebApproved =
      status.includes("website") &&
      !status.includes("website_pending");

    if (hasWebApproved) {
      parts.push("Web");
    }

    // Đã xác thực đủ 3 mục
    if (parts.length === 3) {
      return {
        class: "status-approved",
        text: "Đã xác thực toàn bộ (100đ) ✓",
      };
    }

    // Website đang chờ Admin duyệt
    if (status.includes("website_pending")) {
      return {
        class: "status-pending",
        text: `Đang chờ Admin duyệt Web (${profileData?.point ?? 90}đ)`,
      };
    }

    // Xác thực một phần
    return {
      class: "status-partial",
      text: `Đã xác thực ${parts.join(", ")} (${profileData?.point ?? 80}đ)`,
    };
  };

  if (loading) return <div className="profile-wrapper"><div style={{textAlign: 'center', padding: '40px'}}>Đang tải dữ liệu...</div></div>;
  if (!profileData) return null;
  const isAdmin = profileData.role === 'ADMIN';
  const isRecruiter = profileData.role === 'recruiter';
  const badge = renderStatusBadge(profileData.status);
  
  const isNameVerified = profileData.status?.includes('name');
  const isTaxVerified = profileData.status?.includes('tax');
  const isWebVerified = profileData.status?.includes('website');
  
  const canVerifyTax = !!profileData.companyName;
  const canVerifyWeb = !!profileData.companyName && !!profileData.taxCode;
  
  const currentPoint = profileData.point ?? 80;
  const isLowTrust = isRecruiter && currentPoint <= 90;

  const cvExt = profileData.cvFileName ? profileData.cvFileName.substring(profileData.cvFileName.lastIndexOf('.')).toLowerCase() : '';
  const isCvPdf = cvExt === '.pdf';

  const isVipActive = profileData.vipStatus === 1 && profileData.vipUntil && new Date(profileData.vipUntil) > new Date();

  return (
    <div className="profile-wrapper">
      
      {/* Header Profile Glass */}
      <div className="profile-header-glass">
        {isRecruiter ? (
          <div
            className="profile-avatar-placeholder profile-avatar-logo"
            onClick={() => logoInputRef.current?.click()}
          >
            {profileData.companyLogo ? (
              <img src={getLogoUrl(profileData.companyLogo)} alt="Company logo" />
            ) : (
              <span>{profileData.name ? profileData.name.charAt(0).toUpperCase() : 'U'}</span>
            )}
            <div className="avatar-hover-overlay">
              {uploadingLogo ? '⏳...' : '✏️ Đổi logo công ty'}
            </div>
            <input
              type="file"
              accept=".jpg,.jpeg,.png"
              ref={logoInputRef}
              onChange={handleLogoChange}
              style={{ display: 'none' }}
            />
          </div>
        ) : (
          <div className="profile-avatar-placeholder">
            {profileData.name ? profileData.name.charAt(0).toUpperCase() : 'U'}
          </div>
        )}
        <div className="profile-header-info">
          <h2>{isAdmin ? 'Hồ Sơ Quản Trị Viên' : (isRecruiter ? 'Hồ Sơ Nhà Tuyển Dụng' : 'Hồ Sơ Ứng Viên')}</h2>
          <div className="profile-email">{profileData.email}</div>
          {isRecruiter && (
            <span className={`status-badge-glass ${badge.class}`}>{badge.text}</span>
          )}
        </div>
      </div>

      {!showPasswordModal && notify.msg && (
        <div className={`notify-box nt-${notify.type}`}>
          {notify.type === 'success' && '✅ '}
          {notify.type === 'error' && '❌ '}
          {notify.type === 'info' && 'ℹ️ '}
          {notify.msg}
        </div>
      )}

      {isLowTrust && (
        <div className="trust-warning-banner">
          <strong>⚠️ Điểm tin cậy: {currentPoint}đ</strong> — Cần cung cấp đầy đủ thông tin doanh nghiệp (Tên công ty, Mã số thuế, Website) và vượt qua xác thực AI để đảm bảo đăng tin tuyển dụng không bị hạn chế.
        </div>
      )}

      {/* Main Content Grid */}
      <div className="profile-grid">
        
        {/* Card 1: Account Information */}
        <div className="profile-card">
          <h3>Thông tin tài khoản</h3>
          <div>
            <ReadOnlyField label="Tên tài khoản" value={profileData.name} icon="👤" />
            <ReadOnlyField label="Email đăng ký" value={profileData.email} icon="✉️" />
            <ReadOnlyField label="Vai trò" value={isAdmin ? "Quản trị viên" : (isRecruiter ? "Nhà tuyển dụng" : "Ứng viên")} icon="🛡️" />
            {isRecruiter && <ReadOnlyField label="Email công ty" value={profileData.companyEmail} icon="🏢" />}
            
            <button 
              className="glass-btn btn-secondary mt-3" 
              onClick={() => { setShowPasswordModal(true); setNotify({ type: '', msg: '' }); }}
              style={{ width: '100%' }}
            >
              🔐 Thay đổi mật khẩu
            </button>
          </div>
        </div>

        {/* Card 2: Verification / Contact */}
        <div className="profile-card">
          <h3>{isAdmin ? 'Thông tin quản trị' : (isRecruiter ? 'Thông tin doanh nghiệp (AI)' : 'Thông tin liên hệ & CV')}</h3>
          
          {isAdmin ? (
            <div>
              <ReadOnlyField label="Quyền hạn" value="Quản trị toàn bộ hệ thống (Full Access)" icon="👑" />
              <ReadOnlyField label="Trạng thái" value="Đang hoạt động" icon="🟢" />
            </div>
          ) : isRecruiter ? (
            <div>
                  <div className="profile-field-modern">
                    <span className="field-label-modern">⭐ Trạng thái VIP:</span>
                    {isVipActive ? (
                      <div className="field-value-display">
                        Đang VIP — hết hạn: {new Date(profileData.vipUntil).toLocaleDateString('vi-VN')}
                      </div>
                    ) : (
                      <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
                        <button className="glass-btn btn-primary" disabled={upgrading} onClick={() => handleUpgradeVip(7)}>
                          {upgrading ? '⏳...' : '7 ngày - 50.000đ'}
                        </button>
                        <button className="glass-btn btn-primary" disabled={upgrading} onClick={() => handleUpgradeVip(30)}>
                          {upgrading ? '⏳...' : '30 ngày - 150.000đ'}
                        </button>
                      </div>
                    )}
                    <VipHistoryModal
                      userName={profileData.name}
                      email={profileData.email}
                      role={isAdmin ? 'Quản trị viên' : (isRecruiter ? 'Nhà tuyển dụng' : 'Ứng viên')}
                      companyEmail={profileData.companyEmail}
                    />
                  </div>
              <div className="verify-row-layout">
                <EditableField label="Tên công ty" value={profileData.companyName} onSave={(val) => handleUpdateField('companyName', val)} icon="🏢" />
                <button className="glass-btn btn-primary" onClick={() => handleVerifySingleField('companyName', profileData.companyName)} disabled={!!aiLoadingField}>
                  {aiLoadingField === 'companyName' ? '⏳...' : (isNameVerified ? '✅ Đã duyệt' : 'Duyệt')}
                </button>
              </div>

              <div className="verify-row-layout">
                <EditableField label="Mã số thuế (MST)" value={profileData.taxCode} onSave={(val) => handleUpdateField('taxCode', val)} icon="📑" />
                <button className="glass-btn btn-primary" onClick={() => handleVerifySingleField('taxCode', profileData.taxCode)} disabled={!canVerifyTax || !!aiLoadingField}>
                  {aiLoadingField === 'taxCode' ? '⏳...' : (isTaxVerified ? '✅ Đã duyệt' : 'Duyệt')}
                </button>
              </div>

              <div className="verify-row-layout">
                <EditableField label="Website công ty" value={profileData.websiteUrl} onSave={(val) => handleUpdateField('websiteUrl', val)} icon="🌐" />
                <button className="glass-btn btn-primary" onClick={() => handleVerifySingleField('websiteUrl', profileData.websiteUrl)} disabled={!canVerifyWeb || !!aiLoadingField}>
                  {aiLoadingField === 'websiteUrl' ? '⏳...' : (isWebVerified ? '✅ Đã duyệt' : 'Duyệt')}
                </button>
              </div>
            </div>
          ) : (
            <div>
              <EditableField label="Số điện thoại" value={profileData.phone} onSave={(val) => handleUpdateField('phone', val)} icon="📞" />
              <EditableField label="Địa chỉ" value={profileData.address} onSave={(val) => handleUpdateField('address', val)} icon="📍" />
              
              <div className="cv-section-modern">
                <span className="field-label-modern">📎 CV đính kèm:</span>
                
                {profileData.cvFileName ? (
                  <div style={{ marginTop: '10px', marginBottom: '16px' }}>
                    {isCvPdf ? (
                      <button type="button" className="cv-link" onClick={openCvPreview} disabled={cvBlobLoading}>
                        {cvBlobLoading ? '⏳ Đang tải bản xem trước...' : '📄 Xem CV hiện tại (PDF)'}
                      </button>
                    ) : (
                      <button type="button" className="cv-link" onClick={downloadCvWord} disabled={cvBlobLoading}>
                        {cvBlobLoading ? '⏳ Đang tải file...' : '📄 Tải xuống CV (Word)'}
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="field-empty" style={{ margin: '8px 0 16px' }}>
                    (Chưa có CV nào được tải lên)
                  </div>
                )}

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx"
                    onChange={handleCvChange}
                    id="cv-file-input"
                    style={{ display: 'none' }}
                  />
                  <label htmlFor="cv-file-input" className="cv-choose-btn">
                    Tải file mới...
                  </label>
                  {cvFile && <span className="cv-filename" title={cvFile.name}>{cvFile.name}</span>}
                  
                  <button
                    className="glass-btn btn-primary"
                    onClick={handleUploadCv}
                    disabled={!cvFile || uploadingCv}
                    style={{ marginLeft: 'auto', padding: '8px 16px', height: '40px' }}
                  >
                    {uploadingCv ? '⏳...' : 'Lưu CV'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Password Modal */}
      {showPasswordModal && (
        <div className="cv-preview-overlay" onClick={() => { setShowPasswordModal(false); setNotify({ type: '', msg: '' }); }}>
          <div className="cv-preview-modal" 
                style={{ maxWidth: '420px', height: 'auto', padding: '0 0 20px 0' }} 
                onClick={(e) => e.stopPropagation()}>
            <div className="cv-preview-header">
              <span style={{ fontSize: '18px' }}>Đổi Mật Khẩu</span>
              <button className="cv-preview-close" onClick={() => { setShowPasswordModal(false); setNotify({ type: '', msg: '' }); }}>✕</button>
            </div>
            
            {notify.msg && (
              <div className={`notify-box nt-${notify.type}`} style={{ margin: '20px 24px 0', marginBottom: '-4px' }}>
                {notify.type === 'success' && '✅ '}
                {notify.type === 'error' && '❌ '}
                {notify.type === 'info' && 'ℹ️ '}
                {notify.msg}
              </div>
            )}
            
            <form onSubmit={handlePasswordChange} style={{ padding: '24px 24px 0', display: 'flex', flexDirection: 'column', gap: '18px' }}>
              <div>
                <label className="field-label-modern">Mật khẩu hiện tại</label>
                <input
                  type="password"
                  className="field-input-modern"
                  value={passWordData.oldPassword}
                  onChange={(e) => setPassWordData({ ...passWordData, oldPassword: e.target.value })}
                  required
                />
              </div>

              <div>
                <label className="field-label-modern">Mật khẩu mới</label>
                <input
                  type="password"
                  className="field-input-modern"
                  value={passWordData.newPassword}
                  onChange={(e) => setPassWordData({ ...passWordData, newPassword: e.target.value })}
                  required
                />
              </div>

              <div>
                <label className="field-label-modern">Xác nhận mật khẩu</label>
                <input
                  type="password"
                  className="field-input-modern"
                  value={passWordData.confirmPassword}
                  onChange={(e) => setPassWordData({ ...passWordData, confirmPassword: e.target.value })}
                  required
                />
              </div>

              <button 
                type="submit" 
                className="glass-btn btn-primary" 
                style={{ width: '100%', marginTop: '8px', padding: '12px' }}
                disabled={passWordLoading}
              >
                {passWordLoading ? '⏳ Đang xử lý...' : 'Lưu Thay Đổi'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* CV Preview Modal */}
      {showCvPreview && (
        <div className="cv-preview-overlay" onClick={closeCvPreview}>
          <div className="cv-preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="cv-preview-header">
              <span style={{ fontSize: '18px' }}>Xem trước CV</span>
              <button className="cv-preview-close" onClick={closeCvPreview}>✕</button>
            </div>
            {cvBlobUrl && <iframe src={cvBlobUrl} title="CV Preview" className="cv-preview-frame" />}
          </div>
        </div>
      )}

    </div>
  );
}

export default Profile;