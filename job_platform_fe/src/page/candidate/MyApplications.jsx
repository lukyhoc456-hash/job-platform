import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { decodeToken } from '../../service/api';

function MyApplications() {
  const navigate = useNavigate();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  const token = localStorage.getItem('accessToken');
  const user = token ? decodeToken(token) : null;

  useEffect(() => {
    if (!user || user.role !== 'candidate') {
      navigate('/login');
      return;
    }
    fetchApplications();
  }, []);

  const fetchApplications = async () => {
    try {
      const res = await api.get('/applications/my-applications');
      if (res.data.success) {
        setApplications(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 1: return <span className="badge badge-active">✅ Đã trúng tuyển</span>;
      case -1: return <span className="badge badge-inactive">❌ Đã từ chối</span>;
      default: return <span className="badge badge-pending">⏳ Đang chờ duyệt</span>;
    }
  };

  const getLogoUrl = (logoPath) => {
    if (!logoPath) return null;
    if (logoPath.startsWith('http')) return logoPath;
    if (logoPath.startsWith('/images/')) return logoPath;
    const baseUrl = import.meta.env.VITE_API_BASE_URL.replace('/api/v1', '');
    return `${baseUrl}${logoPath}`;
  };

  return (
    <div style={{ backgroundColor: '#f5f7fa', minHeight: 'calc(100vh - 70px)', padding: '40px 0' }}>
      <div className="container" style={{ maxWidth: '1000px', margin: '0 auto', padding: '0 20px' }}>
        
        {/* Header */}
        <div style={{ marginBottom: '30px' }}>
          <h2 style={{ fontSize: '28px', fontWeight: '700', color: '#121212', marginBottom: '8px' }}>Việc làm của tôi</h2>
          <p style={{ color: '#666', fontSize: '15px', margin: 0 }}>Theo dõi hồ sơ và tiến độ ứng tuyển của bạn</p>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', gap: '30px', borderBottom: '1px solid #e2e8f0', marginBottom: '30px' }}>
          <div style={{ paddingBottom: '12px', borderBottom: '3px solid #ed1b2f', color: '#ed1b2f', fontWeight: '600', fontSize: '16px', cursor: 'pointer' }}>
            Đã ứng tuyển ({applications.length})
          </div>
          <div style={{ paddingBottom: '12px', color: '#64748b', fontWeight: '500', fontSize: '16px', cursor: 'pointer' }}>
            Việc làm đã lưu (0)
          </div>
        </div>

        {loading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '60px 0' }}><div className="spinner"></div></div>
        ) : applications.length === 0 ? (
          <div style={{ backgroundColor: '#fff', padding: '80px 20px', borderRadius: '12px', textAlign: 'center', border: '1px solid #e2e8f0' }}>
            <div style={{ fontSize: '64px', marginBottom: '20px' }}>📝</div>
            <h3 style={{ fontSize: '20px', fontWeight: '700', color: '#121212', marginBottom: '12px' }}>Chưa có công việc nào ứng tuyển</h3>
            <p style={{ color: '#64748b', marginBottom: '30px' }}>Bạn chưa nộp hồ sơ vào công việc nào. Cùng tìm kiếm công việc phù hợp ngay nhé!</p>
            <button className="btn btn-primary" onClick={() => navigate('/jobs')} style={{ padding: '12px 24px', borderRadius: '8px', fontWeight: '600' }}>
              🔍 Tìm việc làm ngay
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {applications.map(app => (
              <div key={app.id} style={{ backgroundColor: '#fff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '24px', display: 'flex', gap: '24px', alignItems: 'flex-start', boxShadow: '0 2px 8px rgba(0,0,0,0.02)', transition: 'transform 0.2s', cursor: 'pointer' }} onClick={() => navigate(`/jobs/${app.jobPost.id}`)} onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-2px)'} onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}>
                
                {/* Company Logo */}
                <div style={{ width: '80px', height: '80px', borderRadius: '8px', border: '1px solid #f1f5f9', backgroundColor: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '4px', flexShrink: 0 }}>
                  {app.jobPost.recruiter.logo ? (
                    <img src={getLogoUrl(app.jobPost.recruiter.logo)} alt={app.jobPost.recruiter.companyName} style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} />
                  ) : (
                    <span style={{ fontSize: '32px', fontWeight: 'bold', color: '#ed1b2f' }}>{app.jobPost.recruiter.companyName.charAt(0).toUpperCase()}</span>
                  )}
                </div>

                {/* Job Info */}
                <div style={{ flex: 1 }}>
                  <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#121212', marginBottom: '6px' }}>{app.jobPost.title}</h3>
                  <div style={{ fontSize: '15px', color: '#475569', marginBottom: '12px' }}>{app.jobPost.recruiter.companyName}</div>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontSize: '13px', color: '#64748b' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span style={{ fontSize: '16px' }}>🕒</span> Đã nộp: {new Date(app.createdAt).toLocaleDateString('vi-VN')}
                    </span>
                    {app.cvPath && (
                      <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <span style={{ fontSize: '16px' }}>📄</span> CV đính kèm
                      </span>
                    )}
                  </div>
                </div>

                {/* Status Badge */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '12px' }}>
                  {app.status === 1 ? (
                    <span style={{ padding: '6px 16px', backgroundColor: '#dcfce7', color: '#15803d', borderRadius: '20px', fontWeight: '600', fontSize: '14px', border: '1px solid #bbf7d0' }}>✅ Trúng tuyển</span>
                  ) : app.status === -1 ? (
                    <span style={{ padding: '6px 16px', backgroundColor: '#fee2e2', color: '#b91c1c', borderRadius: '20px', fontWeight: '600', fontSize: '14px', border: '1px solid #fecaca' }}>❌ Bị từ chối</span>
                  ) : (
                    <span style={{ padding: '6px 16px', backgroundColor: '#fff7ed', color: '#c2410c', borderRadius: '20px', fontWeight: '600', fontSize: '14px', border: '1px solid #ffedd5' }}>⏳ Chờ nhà tuyển dụng</span>
                  )}
                </div>
                
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default MyApplications;
