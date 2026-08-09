import { useState } from 'react';
import api from '../service/api';

function VipHistoryModal({ userName, email, role, companyEmail }) {
  const [open, setOpen] = useState(false);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);

  const openModal = async () => {
    setOpen(true);
    setLoading(true);
    try {
      const res = await api.get('/payment/vnpay/history');
      setHistory(res.data?.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const daysLeft = (expireAt) => {
    if (!expireAt) return '-';
    const diff = new Date(expireAt) - new Date();
    const d = Math.ceil(diff / (1000 * 60 * 60 * 24));
    return d > 0 ? `Còn ${d} ngày` : 'Đã hết hạn';
  };

  const fmt = (dt) => (dt ? new Date(dt).toLocaleDateString('vi-VN') : '-');

  return (
    <>
      <button
        type="button"
        className="glass-btn btn-secondary"
        onClick={openModal}
        style={{ marginTop: 8 }}
      >
        📜 Lịch sử thanh toán VIP
      </button>

      {open && (
        <div className="cv-preview-overlay" onClick={() => setOpen(false)}>
          <div
            className="cv-preview-modal"
            style={{ maxWidth: 700, height: 'auto', padding: '0 0 20px 0' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="cv-preview-header">
              <span style={{ fontSize: 18 }}>Lịch sử thanh toán VIP</span>
              <button className="cv-preview-close" onClick={() => setOpen(false)}>✕</button>
            </div>

            <div style={{ padding: '20px 24px 0' }}>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: 8,
                  fontSize: 14,
                  marginBottom: 16,
                  background: 'rgba(0,0,0,0.03)',
                  padding: 12,
                  borderRadius: 10,
                }}
              >
                <div><span style={{ color: '#888' }}>Tên tài khoản:</span> {userName}</div>
                <div><span style={{ color: '#888' }}>Email:</span> {email}</div>
                <div><span style={{ color: '#888' }}>Vai trò:</span> {role}</div>
                <div><span style={{ color: '#888' }}>Email công ty:</span> {companyEmail || '-'}</div>
              </div>

              {loading ? (
                <p style={{ color: '#888', fontSize: 14 }}>Đang tải...</p>
              ) : history.length === 0 ? (
                <p style={{ color: '#888', fontSize: 14 }}>Chưa có giao dịch VIP nào.</p>
              ) : (
                <table style={{ width: '100%', fontSize: 14, borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid #ddd', textAlign: 'left', color: '#666' }}>
                      <th style={{ padding: '8px 4px' }}>Ngày đăng ký</th>
                      <th style={{ padding: '8px 4px' }}>Số ngày</th>
                      <th style={{ padding: '8px 4px' }}>Ngày hết hạn</th>
                      <th style={{ padding: '8px 4px' }}>Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((h, i) => (
                      <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                        <td style={{ padding: '8px 4px' }}>{fmt(h.paidAt)}</td>
                        <td style={{ padding: '8px 4px' }}>{h.days} ngày</td>
                        <td style={{ padding: '8px 4px' }}>{fmt(h.expireAt)}</td>
                        <td style={{ padding: '8px 4px' }}>{daysLeft(h.expireAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default VipHistoryModal;