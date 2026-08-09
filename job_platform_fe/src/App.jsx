import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import AiChatWidget from './components/AiChatWidget';
import BugReportWidget from './components/BugReportWidget';

// Pages
import Home from './page/home/Home';
import Login from './page/login/Login';
import Register from './page/register/Register';
import JobList from './page/jobs/JobList';
import JobListVip  from './page/jobs/JobListVip';
import JobDetail from './page/jobs/JobDetail';
import CompanyList from './page/companies/CompanyList';
import CompanyDetail from './page/companies/CompanyDetail';
import Profile from './page/proifle/Profile';

// Candidate
import CvManager from './page/candidate/CvManager';
import MyApplications from './page/candidate/MyApplications';

// Recruiter
import RecruiterDashboard from './page/recruiter/RecruiterDashboard';
import PostJob from './page/recruiter/PostJob';
import JobApplicants from './page/recruiter/JobApplicants';

// Admin
import AdminDashboard from './page/admin/AdminDashboard';

// Shared
import BugReport from './page/bugReport/BugReport';

function App() {
  return (
    <BrowserRouter>
      <Navbar />
      
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/jobs" element={<JobList />} />
          <Route path="/jobs/vip" element={<JobListVip />} />
          <Route path="/jobs/:id" element={<JobDetail />} />
          <Route path="/companies" element={<CompanyList />} />
          <Route path="/companies/:id" element={<CompanyDetail />} />
          
          {/* Shared Authenticated Routes */}
          <Route path="/profile" element={<Profile />} />
          <Route path="/bug-report" element={<BugReport />} />

          {/* Candidate Routes */}
          <Route path="/cv" element={<CvManager />} />
          <Route path="/my-applications" element={<MyApplications />} />

          {/* Recruiter Routes */}
          <Route path="/my-posts" element={<RecruiterDashboard />} />
          <Route path="/post-job" element={<PostJob />} />
          <Route path="/edit-job/:id" element={<PostJob />} /> {/* Dùng chung form */}
          <Route path="/job-applicants/:jobId" element={<JobApplicants />} />

          {/* Admin Routes */}
          <Route path="/admin" element={<AdminDashboard />} />
        </Routes>
      </main>

      <Footer />
      <AiChatWidget />
      <BugReportWidget />
    </BrowserRouter>
  );
}

export default App;