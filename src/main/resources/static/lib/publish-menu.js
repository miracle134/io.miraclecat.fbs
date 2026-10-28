function publishMenuInit() {
    /* 슬라이드 메뉴 토글 */
    const hamburger = document.getElementById('hamburger');
    const sideMenu = document.getElementById('sideMenu');
    const wrap = document.getElementById('wrap');

    if (hamburger && sideMenu && wrap) {
        hamburger.addEventListener('click', () => {
            sideMenu.classList.toggle('open');
            wrap.classList.toggle('shift');
        });
    }

    /* 상위 메뉴 선택*/
    document.querySelectorAll('.toggle-menu').forEach(item => {
        item.addEventListener('click', function() {
            const menuId = this.dataset.parentMenu;
            switch (menuId) {
                // 대시보드
                case '/':
                    location.href="/";
                    break;
            }

            const menu = this.querySelector('.submenu');
            const arrow = this.querySelector('.arrow');
            const header = arrow.parentElement;

            if (menu.classList.contains('open')) {
                menu.classList.remove('open');
                arrow.classList.remove('rotate');
                header.classList.remove('active');
            } else {
                // Close Current Menu
                document.querySelectorAll('.submenu.open').forEach(m => m.classList.remove('open'));
                document.querySelectorAll('.arrow.rotate').forEach(a => a.classList.remove('rotate'));
                document.querySelectorAll('.menu-header.active').forEach(h => h.classList.remove('active'));

                // Open Menu
                menu.classList.add('open');
                arrow.classList.add('rotate');
                header.classList.add('active');
            }

        });
    });

    /* 서브 메뉴 선택*/
    document.querySelectorAll('.submenu-item').forEach(item => {
        item.addEventListener('click', function() {
            location.href = this.dataset.location;
        });
    });

    /* 메뉴 활성화 */
    const currentPath = window.location.pathname;
    document.querySelectorAll('.toggle-menu').forEach(parent => {
        const parentMenu = parent.dataset.parentMenu;
        if(isActive(currentPath, parentMenu)) {
            parent.querySelector('.submenu').classList.add('open');
            parent.querySelector('.arrow').classList.add('rotate');
            parent.querySelector('.menu-header').classList.add('active');

            parent.querySelectorAll('.submenu-item').forEach(item => {
                const loc = item.dataset.location;
                if (currentPath === loc) {
                    item.classList.add('active');
                }
            });
        }
    });

}

function isActive(current, menu) {
  return current === menu || current.startsWith(menu + '/');
}