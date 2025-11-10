// theme-switcher.js

(function() {
    // 1. 获取保存在 localStorage 的主题
    const currentTheme = localStorage.getItem('theme');

    // 2. 获取用户操作系统的偏好
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    // 3. 决定应用哪个主题
    let themeToSet;
    if (currentTheme) {
        // 如果用户手动设置过，则使用用户的设置
        themeToSet = currentTheme;
    } else {
        // 否则，根据系统偏好设置
        themeToSet = prefersDark ? 'dark' : 'light';
    }

    // 4. 在 <html> 标签上设置 data-theme 属性
    document.documentElement.setAttribute('data-theme', themeToSet);

    // 5. 将切换功能暴露给全局，以便按钮可以调用
    window.toggleTheme = function() {
        const html = document.documentElement;
        const newTheme = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';

        html.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme); // 保存用户选择

        // 触发一个自定义事件，其他JS可以监听这个变化
        window.dispatchEvent(new Event('themeChanged'));
    };

    // 6. (可选) 监听系统主题变化
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        // 仅当用户没有手动设置过主题时，才跟随系统
        if (!localStorage.getItem('theme')) {
            const newTheme = e.matches ? 'dark' : 'light';
            document.documentElement.setAttribute('data-theme', newTheme);
        }
    });

})();