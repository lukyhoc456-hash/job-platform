import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../../service/api';
import JobCard from '../../components/JobCard';
import './JobList.css';

const PAGE_SIZE = 20;

function JobListVip() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);

  const fetchVipJobs = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append('vip', 'true');
      params.append('page', page);
      params.append('size', PAGE_SIZE);

      const res = await api.get(`/jobs?${params.toString()}`);
      if (res.data.success) {
        setJobs(res.data.data?.content || []);
        setTotalPages(res.data.data?.totalPages || 0);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVipJobs();
    window.scrollTo({ top: 0, behavior: 'smooth' });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  return (
    <div className="job-list-page">
      <div className="job-list-container" style={{ display: 'block' }}>
        <main className="job-list-main" style={{ maxWidth: 900, margin: '0 auto' }}>
            <Link
                to="/jobs"
                style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                color: '#475569',
                fontSize: 14,
                fontWeight: 600,
                textDecoration: 'none',
                marginBottom: 16,
                }}
            >
            ← Quay lại danh sách việc làm
          </Link>
          <div className="job-list-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
            <div>
              <h2>Việc làm từ Đối Tác VIP</h2>
              <p className="job-list-count">
                {jobs.length > 0 ? `${jobs.length} việc làm xác thực từ đối tác` : 'Hiện chưa có việc làm nào từ đối tác VIP.'}
              </p>
            </div>
            <Link to="/jobs" className="filter-btn-clear" style={{ textDecoration: 'none', display: 'inline-block', width: 'auto', padding: '10px 20px' }}>
              ← Xem tất cả việc làm
            </Link>
          </div>

          {loading ? (
            <div className="company-loading" style={{ marginTop: '20px' }}>
              Đang tải danh sách việc làm từ đối tác...
            </div>
          ) : (
            <>
              <div>
                {jobs.map((job) => (
                  <JobCard key={job.id} job={job} />
                ))}
              </div>

              {totalPages > 1 && (
                <div className="job-pagination">
                  <button
                    className="job-pagination-btn wide"
                    disabled={page === 0}
                    onClick={() => setPage(p => p - 1)}
                  >
                    ← Trước
                  </button>

                  {Array.from({ length: totalPages }, (_, i) => {
                    if (i === 0 || i === totalPages - 1 || Math.abs(page - i) <= 1) {
                      return (
                        <button
                          key={i}
                          className={`job-pagination-btn ${page === i ? 'active' : ''}`}
                          onClick={() => setPage(i)}
                        >
                          {i + 1}
                        </button>
                      );
                    } else if (i === 1 && page > 2) {
                      return <span key={i} style={{ display: 'flex', alignItems: 'center', color: '#94a3b8' }}>...</span>;
                    } else if (i === totalPages - 2 && page < totalPages - 3) {
                      return <span key={i} style={{ display: 'flex', alignItems: 'center', color: '#94a3b8' }}>...</span>;
                    }
                    return null;
                  })}

                  <button
                    className="job-pagination-btn wide"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage(p => p + 1)}
                  >
                    Sau →
                  </button>
                </div>
              )}
            </>
          )}
        </main>
      </div>
    </div>
  );
}

export default JobListVip;