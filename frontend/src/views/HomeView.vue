<template>
  <div class="home-page">
    <!-- Hero Banner -->
    <section class="hero-section">
      <div class="container hero-container">
        <h1 class="hero-title">Đặt Lịch Khám Bệnh Nhanh Chóng & An Toàn</h1>
        <p class="hero-subtitle">Kết nối với các Bác sĩ hàng đầu, đặt suất khám trực tuyến 24/7 không cần chờ đợi.</p>
        
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="Tìm theo tên bác sĩ..." 
            class="search-input"
          />
        </div>
      </div>
    </section>

    <div class="container main-content">
      <!-- Specialty Filter Pills -->
      <section class="specialty-section">
        <h2 class="section-title">Chuyên Khoa Khám</h2>
        <div class="specialty-pills">
          <button 
            :class="['pill', { active: selectedSpecialty === null }]"
            @click="filterSpecialty(null)"
          >
            Tất Cả
          </button>
          <button 
            v-for="s in specialties" 
            :key="s.id"
            :class="['pill', { active: selectedSpecialty === s.id }]"
            @click="filterSpecialty(s.id)"
          >
            {{ s.name }}
          </button>
        </div>
      </section>

      <!-- Doctor Grid -->
      <section class="doctors-section">
        <h2 class="section-title">Đội Ngũ Bác Sĩ</h2>
        
        <div v-if="loading" class="loading-spinner">Đang tải danh sách bác sĩ...</div>
        
        <div v-else-if="filteredDoctors.length > 0" class="doctor-grid">
          <DoctorCard 
            v-for="doc in filteredDoctors" 
            :key="doc.userId" 
            :doctor="doc" 
          />
        </div>

        <div v-else class="empty-doctors">
          Không tìm thấy bác sĩ phù hợp với tìm kiếm của bạn.
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useContainer } from '../core/container';
import DoctorCard from '../components/DoctorCard.vue';

const { facade } = useContainer();

const specialties = ref([]);
const doctors = ref([]);
const selectedSpecialty = ref(null);
const searchQuery = ref('');
const loading = ref(true);

onMounted(async () => {
  try {
    const data = await facade.loadHome();
    specialties.value = data.specialties;
    doctors.value = data.doctors;
  } catch (err) {
    console.error('Failed to load data:', err);
  } finally {
    loading.value = false;
  }
});

const filterSpecialty = async (specialtyId) => {
  selectedSpecialty.value = specialtyId;
  loading.value = true;
  try {
    doctors.value = await facade.listDoctors(specialtyId);
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const filteredDoctors = computed(() => {
  if (!searchQuery.value.trim()) return doctors.value;
  const q = searchQuery.value.toLowerCase();
  return doctors.value.filter(d => d.fullName.toLowerCase().includes(q));
});
</script>

<style scoped>
.hero-section {
  background: linear-gradient(135deg, #0d9488 0%, #0f766e 100%);
  color: #ffffff;
  padding: 4rem 0 5rem;
  text-align: center;
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 1rem;
}

.hero-subtitle {
  font-size: 1.15rem;
  opacity: 0.9;
  max-width: 600px;
  margin: 0 auto 2rem;
}

.search-box {
  max-width: 500px;
  margin: 0 auto;
}

.search-input {
  width: 100%;
  padding: 1rem 1.5rem;
  font-size: 1rem;
  border-radius: 9999px;
  border: none;
  box-shadow: var(--shadow-lg);
  outline: none;
}

.main-content {
  padding-top: 3rem;
  padding-bottom: 5rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 1.25rem;
}

.specialty-section {
  margin-bottom: 3rem;
}

.specialty-pills {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.pill {
  padding: 0.5rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 600;
  border-radius: 9999px;
  border: 1.5px solid var(--border-color);
  background: #ffffff;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s;
}

.pill:hover, .pill.active {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
}

.doctor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1.5rem;
}

.loading-spinner, .empty-doctors {
  text-align: center;
  padding: 3rem;
  color: var(--text-secondary);
}
</style>
