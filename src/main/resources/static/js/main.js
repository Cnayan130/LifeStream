// main.js
$(document).ready(function() {
    // Initialize Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    });

    // Initialize Bootstrap popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    const popoverList = popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl)
    });

    // Scroll to top button
    $(window).scroll(function() {
        if ($(this).scrollTop() > 300) {
            $('#scrollToTop').fadeIn();
        } else {
            $('#scrollToTop').fadeOut();
        }
    });

    $('#scrollToTop').click(function() {
        $('html, body').animate({scrollTop: 0}, 800);
        return false;
    });

    // Mobile nav toggle
    $('.navbar-toggler').click(function() {
        $(this).toggleClass('open');
    });

    // Confirm dialog for delete actions
    $('.confirm-delete').click(function(e) {
        if (!confirm('确定要删除吗？此操作不可撤销。')) {
            e.preventDefault();
        }
    });

    // Form validation
    const forms = document.querySelectorAll('.needs-validation');
    if (forms.length) {
        Array.from(forms).forEach(form => {
            form.addEventListener('submit', event => {
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            }, false);
        });
    }

    // Handle flash messages
    const flashMessage = document.querySelector('.flash-message');
    if (flashMessage) {
        setTimeout(function() {
            $(flashMessage).fadeOut(500);
        }, 5000);
    }

    // Theme toggle (for future use)
    $('#themeToggle').click(function() {
        const currentTheme = $('html').attr('data-theme');
        if (currentTheme === 'dark') {
            $('html').attr('data-theme', 'light');
            $(this).html('<i class="fas fa-moon"></i>');
            localStorage.setItem('theme', 'light');
        } else {
            $('html').attr('data-theme', 'dark');
            $(this).html('<i class="fas fa-sun"></i>');
            localStorage.setItem('theme', 'dark');
        }
    });

    // Initialize theme from local storage (for future use)
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
        $('html').attr('data-theme', savedTheme);
        if (savedTheme === 'dark') {
            $('#themeToggle').html('<i class="fas fa-sun"></i>');
        }
    }

    // Image preview on upload
    $('#imageUpload').change(function() {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function(e) {
                $('#imagePreview').attr('src', e.target.result).show();
            }
            reader.readAsDataURL(file);
        }
    });

    // Auto resize textarea
    $('textarea.auto-resize').each(function () {
        this.setAttribute('style', 'height:' + (this.scrollHeight) + 'px;overflow-y:hidden;');
    }).on('input', function () {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });

    // AJAX form submission
    $('.ajax-form').submit(function(e) {
        e.preventDefault();
        const form = $(this);
        const submitBtn = form.find('[type="submit"]');
        const originalBtnText = submitBtn.html();

        submitBtn.html('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 处理中...').prop('disabled', true);

        $.ajax({
            url: form.attr('action'),
            type: form.attr('method'),
            data: form.serialize(),
            success: function(response) {
                if (response.redirect) {
                    window.location.href = response.redirect;
                } else if (response.message) {
                    showAlert('success', response.message);
                    form[0].reset();
                }
            },
            error: function(xhr) {
                let errorMessage = '操作失败，请重试';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                }
                showAlert('danger', errorMessage);
            },
            complete: function() {
                submitBtn.html(originalBtnText).prop('disabled', false);
            }
        });
    });

    // Show alert function
    function showAlert(type, message) {
        const alertHtml = `
            <div class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;

        const alertContainer = $('#alertContainer');
        if (alertContainer.length) {
            alertContainer.html(alertHtml);
        } else {
            $('main .container').prepend(`<div id="alertContainer">${alertHtml}</div>`);
        }

        // Auto-dismiss after 5 seconds
        setTimeout(function() {
            $('.alert').alert('close');
        }, 5000);
    }

    // Lazy loading images
    if ('loading' in HTMLImageElement.prototype) {
        const lazyImages = document.querySelectorAll('img[loading="lazy"]');
        lazyImages.forEach(img => {
            img.src = img.dataset.src;
        });
    } else {
        // Fallback for browsers that don't support lazy loading
        const lazyImageObserver = new IntersectionObserver(function(entries, observer) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    const lazyImage = entry.target;
                    lazyImage.src = lazyImage.dataset.src;
                    lazyImageObserver.unobserve(lazyImage);
                }
            });
        });

        const lazyImages = document.querySelectorAll('img[data-src]');
        lazyImages.forEach(function(lazyImage) {
            lazyImageObserver.observe(lazyImage);
        });
    }

    // Handle comment replies
    $('.reply-button').click(function() {
        const commentId = $(this).data('comment-id');
        $('#replyForm-' + commentId).slideToggle();
    });

    // Check for unsaved changes before leaving page
    const forms1 = document.querySelectorAll('form.check-changes');
    let formChanged = false;

    forms1.forEach(form => {
        const initialFormState = $(form).serialize();

        $(form).on('change input', function() {
            formChanged = $(this).serialize() !== initialFormState;
        });

        $(form).on('submit', function() {
            formChanged = false;
        });
    });

    if (forms1.length > 0) {
        window.addEventListener('beforeunload', function(e) {
            if (formChanged) {
                e.preventDefault();
                e.returnValue = '您有未保存的更改，确定要离开吗？';
                return e.returnValue;
            }
        });
    }
});

// JWT认证管理
// 统一的认证管理器
// 改进的认证管理器
const AuthManager = {
    TOKEN_KEY: 'token',
    USER_DATA_KEY: 'user_data', // 添加用户数据缓存键

    setToken: function(token, userData) {
        localStorage.setItem(this.TOKEN_KEY, token);

        // 存储用户数据
        if (userData) {
            localStorage.setItem(this.USER_DATA_KEY, JSON.stringify(userData));
        }

        this.setupAuthHeaders();
    },

    getToken: function() {
        return localStorage.getItem(this.TOKEN_KEY);
    },

    getUserData: function() {
        const userData = localStorage.getItem(this.USER_DATA_KEY);
        return userData ? JSON.parse(userData) : null;
    },

    clearToken: function() {
        try {
            // 清除所有相关存储
            localStorage.removeItem(this.TOKEN_KEY);
            localStorage.removeItem(this.USER_DATA_KEY);
            sessionStorage.removeItem('currentUser');

            // 清除请求头
            if ($ && $.ajaxSettings && $.ajaxSettings.headers) {
                delete $.ajaxSettings.headers["Authorization"];
            }

            if (window.axios && window.axios.defaults.headers.common) {
                delete window.axios.defaults.headers.common["Authorization"];
            }

            console.log('Auth state cleared successfully');
            this.updateUI();
        } catch (error) {
            console.error('Error clearing token:', error);
        }
    },

    setupAuthHeaders: function() {
        const token = this.getToken();
        if (token) {
            // 设置jQuery的全局请求头
            $.ajaxSetup({
                beforeSend: function(xhr) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + token);
                }
            });

            // 同时也直接设置headers属性，以兼容不同的jQuery版本和使用场景
            $.ajaxSetup({
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            });

            // 设置axios的全局请求头
            if (window.axios) {
                axios.defaults.headers.common['Authorization'] = 'Bearer ' + token;
            }

            console.log('Auth headers setup complete with token');
        }
    },

    validateToken: async function() {
        const token = this.getToken();
        if (!token) {
            console.log('No token found');
            return false;
        }

        try {
            console.log('Validating token...');
            const response = await $.ajax({
                url: '/api/user/me',
                type: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                // 添加缓存控制
                cache: false
            });

            if (response) {
                console.log('Token valid, user:', response.username);

                // 更新用户数据存储
                localStorage.setItem(this.USER_DATA_KEY, JSON.stringify(response));

                // 更新UI元素
                $('.username').text(response.username);
                if (response.avatarUrl) {
                    $('#userDropdown img').attr('src', response.avatarUrl + '?t=' + new Date().getTime());
                }
                return true;
            }
            console.log('Token validation failed - empty response');
            return false;
        } catch (error) {
            console.error('Token validation error:', error);
            this.clearToken();
            return false;
        }
    },

    setupAuthInterceptors: function() {
        // 处理认证错误，重定向到登录页面
        $(document).ajaxError((event, jqXHR) => {
            if (jqXHR.status === 401 || jqXHR.status === 403) {
                console.log('Auth error detected:', jqXHR.status);
                this.clearToken();
                const currentPath = window.location.pathname;
                window.location.href = `/login?returnUrl=${encodeURIComponent(currentPath)}`;
            }
        });
    },

    initialize: async function() {
        console.log('Initializing AuthManager...');

        // 设置认证头
        this.setupAuthHeaders();

        // 设置拦截器
        this.setupAuthInterceptors();

        // 验证令牌并加载用户数据
        const isValid = await this.validateToken();

        // 设置导航保护
        this.setupNavigation();

        // 更新UI
        this.updateUI();

        console.log('Auth initialization complete, token valid:', isValid);
        return isValid;
    },

    // 修改 AuthManager.setupNavigation 方法
    setupNavigation: function() {
        // 使用事件委托模式，确保动态生成的链接也能被监听
        $(document).on('click', 'a[href^="/posts/new"], a[href^="/dashboard"], a[href^="/albums/new"], a[href^="/profile"]', function(e) {
            const token = AuthManager.getToken();
            const href = $(this).attr('href');

            console.log('Navigation intercepted:', href); // 增加调试日志

            if (!token) {
                e.preventDefault();
                console.log('Protected route access blocked, redirecting to login');
                window.location.href = '/login?returnUrl=' + encodeURIComponent(href);
            } else {
                // 有效的token，让链接正常工作
                console.log('Access granted to:', href);
                // 不阻止默认行为
            }
        });
    },

    updateUI: function() {
        const token = this.getToken();
        console.log('Updating UI, token exists:', !!token);

        if (!token) {
            $('.auth-buttons').removeClass('d-none');
            $('.join').removeClass('d-none');
            $('.user-menu').addClass('d-none');
        } else {
            $('.auth-buttons').addClass('d-none');
            $('.join').addClass('d-none');
            $('.user-menu').removeClass('d-none');

            // 确保显示正确的用户名
            const userData = this.getUserData();
            if (userData && userData.username) {
                $('.username').text(userData.username);
                if (userData.avatarUrl) {
                    $('#userDropdown img').attr('src', userData.avatarUrl + '?t=' + new Date().getTime());
                }
            }
        }
    },

    logout: function() {
        console.log('Logging out...');
        const token = this.getToken();

        // 先清除本地存储，立即响应用户操作
        this.clearToken();

        // 后台异步调用注销端点
        $.ajax({
            url: '/api/auth/logout',
            type: 'POST',
            headers: token ? { 'Authorization': 'Bearer ' + token } : {},
            success: () => {
                console.log('Server logout successful');
            },
            error: (xhr, status, error) => {
                console.error('Server logout error:', error);
            },
            complete: () => {
                // 不管服务器响应如何，都重定向到首页
                window.location.href = '/';
            }
        });
    }
};

// 添加这段代码，处理文档加载后的初始化
$(document).ready(async function () {
    console.log('Document ready, starting authentication...');

    // 初始化模态框
    const modals = document.querySelectorAll('.modal');
    modals.forEach(function(modal) {
        modal.addEventListener('show.bs.modal', function () {
            document.body.style.overflow = 'visible';
            this.style.zIndex = 1060;

            const backdrop = document.querySelector('.modal-backdrop');
            if (backdrop) {
                backdrop.style.zIndex = 1050;
            }
        });

        modal.addEventListener('hidden.bs.modal', function () {
            document.body.style.overflow = 'auto';
        });
    });

    // 初始化认证
    await AuthManager.initialize();

    // 登录表单处理
    $("#loginForm").on('submit', function (e) {
        e.preventDefault();
        console.log('Login form submitted');

        // 获取登录表单数据
        const formData = {
            usernameOrEmail: $('#username').val(),
            password: $('#password').val(),
            rememberMe: $('#rememberMe').is(':checked')
        };

        $.ajax({
            url: '/api/auth/signin',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(formData),
            success: function (response) {
                if (response && response.accessToken) {
                    console.log('Login successful, setting token');
                    // 保存令牌和用户数据
                    AuthManager.setToken(response.accessToken, response.userData);
                    const returnUrl = new URLSearchParams(location.search).get('returnUrl') || '/';
                    console.log('Redirecting to:', returnUrl);
                    window.location.href = returnUrl;
                }
            },
            error: function (xhr) {
                console.error('Login failed:', xhr);
                let errorMsg = '登录失败: 用户名或密码错误';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMsg = '登录失败: ' + xhr.responseJSON.message;
                }
                alert(errorMsg);
            }
        });
    });

    // 退出登录处理
    $(document).on('click', '#logoutBtn', function (e) {
        e.preventDefault();
        AuthManager.logout();
    });
});