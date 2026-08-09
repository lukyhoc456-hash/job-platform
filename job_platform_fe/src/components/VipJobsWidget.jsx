import { useState } from 'react';
import { Link } from 'react-router-dom';
import './VipJobsWidget.css';

function timeAgo(dateStr) {
  if (!dateStr) return '';
  const diffMs = Date.now() - new Date(dateStr).getTime();
  const hours = Math.floor(diffMs / (1000 * 60 * 60));
  if (hours < 1) return 'Vừa đăng';
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
}

function getLogoUrl(logoPath) {
  if (!logoPath) return null;
  if (logoPath.startsWith('http')) return logoPath;
  if (logoPath.startsWith('/images/')) return logoPath;
  const baseUrl = import.meta.env.VITE_API_BASE_URL.replace('/api/v1', '');
  return `${baseUrl}${logoPath}`;
}

function VipJobsWidget({ jobs, onSeeMore }) {
  const [startIndex, setStartIndex] = useState(0);
  const visible = jobs.slice(startIndex, startIndex + 4);
  const hasMore = startIndex + 4 < jobs.length;

  const handleNext = () => {
    setStartIndex(prev => (hasMore ? prev + 4 : 0));
  };

  if (visible.length === 0) return null;

  return (
    <div className="vip-widget">
      <div className="vip-widget-header">
        <h3>Từ Đối Tác Việc Làm Tốt</h3>
        <div className="vip-widget-badges">
          <span className="vip-badge success">✔ Việc làm xác thực</span>
          <span className="vip-badge success">✔ Phản hồi trong 7 ngày</span>
        </div>
      </div>

      <div className="vip-widget-body">
        <div className="vip-widget-grid">
          {visible.map((job) => (
            <Link to={`/jobs/${job.id}`} key={job.id} className="vip-mini-card">
              <div className="vip-mini-card-top">
                <span className="vip-mini-tag">Tuyển gấp</span>
                <span className="vip-mini-tag partner">Đối Tác</span>
              </div>
              <div className="vip-mini-logo">
                {job.companyLogo ? (
                  <img src={getLogoUrl(job.companyLogo)} alt={job.companyName} />
                ) : (
                  <span>{job.companyName?.charAt(0).toUpperCase() || '?'}</span>
                )}
              </div>
              <h4 className="vip-mini-title">{job.title}</h4>
              <p className="vip-mini-company">{job.companyName}</p>
              <p className="vip-mini-salary">{job.salary || 'Thỏa thuận'}</p>
              <p className="vip-mini-meta">
                <span>{timeAgo(job.createdAt)}</span>
                <span> · {job.applicationCount} lượt liên hệ</span>
              </p>
            </Link>
          ))}
        </div>

        {jobs.length > 4 && (
          <button className="vip-widget-arrow" onClick={handleNext} aria-label="Xem thêm">
            ›
          </button>
        )}
      </div>

      <button className="vip-widget-more" onClick={onSeeMore}>
        Xem thêm tin đăng
      </button>
    </div>
  );
}

export default VipJobsWidget;