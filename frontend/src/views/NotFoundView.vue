<template>
  <main class="workspace not-found-layout">
    <section class="not-found-panel">
      <span class="eyebrow">404</span>
      <h1>페이지를 찾을 수 없어요.</h1>
      <p>주소가 바뀌었거나 삭제된 화면입니다. 아래 바로가기로 주요 기능에 다시 진입할 수 있습니다.</p>
      <div class="not-found-actions">
        <RouterLink class="primary" to="/">홈으로</RouterLink>
        <RouterLink to="/cases">응급 사례</RouterLink>
        <RouterLink to="/videos">응급 영상</RouterLink>
        <RouterLink to="/favorites">찜한 영상</RouterLink>
        <RouterLink :to="{ path: '/users', query: { tab: 'follow' } }">팔로우 관리</RouterLink>
      </div>
    </section>
  </main>
</template>
