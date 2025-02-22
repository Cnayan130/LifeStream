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

    // 添加到 main.js
    document.addEventListener('DOMContentLoaded', function() {
        // 检查并设置 JWT Token
        const token = localStorage.getItem('token');

        if (token) {
            // 在所有 Axios 请求中携带 Token
            axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

            // 验证 Token
            fetch('/api/auth/validate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            })
                .then(response => {
                    if (!response.ok) {
                        // Token 无效，清除
                        localStorage.removeItem('token');
                        delete axios.defaults.headers.common['Authorization'];
                    }
                })
                .catch(error => {
                    console.error('Token 验证错误', error);
                    localStorage.removeItem('token');
                    delete axios.defaults.headers.common['Authorization'];
                });
        }
    });

    document.addEventListener('DOMContentLoaded', function() {
        const modals = document.querySelectorAll('.modal');
        modals.forEach(function(modal) {
            modal.addEventListener('show.bs.modal', function () {
                // 确保模态框可点击
                document.body.style.overflow = 'visible';
                this.style.zIndex = 1060;

                // 调整背景遮罩
                const backdrop = document.querySelector('.modal-backdrop');
                if (backdrop) {
                    backdrop.style.zIndex = 1050;
                }
            });

            modal.addEventListener('hidden.bs.modal', function () {
                document.body.style.overflow = 'auto';
            });
        });
    });
});

// JWT认证管理
// 统一的认证管理器
const AuthManager = {
    // 统一使用 token 作为存储 key
    setToken: function(token) {
        localStorage.setItem('token', token);
        this.setupAuthHeaders();
    },

    getToken: function() {
        return localStorage.getItem('token');
    },

    clearToken: function() {
        try {
            localStorage.removeItem('token');
            localStorage.removeItem('jwtToken'); // Clear possible old version token

            // Safely remove Authorization header
            if ($ && $.ajaxSettings && $.ajaxSettings.headers) {
                delete $.ajaxSettings.headers["Authorization"];
            }

            // Additional safety checks
            this.updateUI();
        } catch (error) {
            console.error('Error clearing token:', error);
        }
    },

    setupAuthHeaders: function() {
        const token = this.getToken();
        if (token) {
            $.ajaxSetup({
                beforeSend: function(xhr) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + token);
                }
            });
        }
    },

    setupAuthInterceptors: function() {
        this.setupAuthHeaders();
        // 处理认证错误
        $(document).ajaxError((event, jqXHR) => {
            if (jqXHR.status === 401 || jqXHR.status === 403) {
                this.clearToken();
                window.location.href = '/login?expired=true';
            }
        });
    },

    updateUI: function() {
        const token = this.getToken();
        if (!token) {
            $('.auth-buttons').removeClass('d-none');
            $('.user-menu').addClass('d-none');
        } else {
            $('.auth-buttons').addClass('d-none');
            $('.user-menu').removeClass('d-none');
        }
    },

    logout: function() {
        $.ajax({
            url: '/api/auth/logout',
            type: 'POST',
            complete: () => {
                this.clearToken();
                location.reload(); // Reload page after clearing token
            },
            error: (xhr, status, error) => {
                console.error('Logout error:', error);
                // Force clear token and reload even if logout request fails
                this.clearToken();
                location.reload();
            }
        });
    }
};

// 只保留一个初始化入口点
$(document).ready(function() {
    AuthManager.setupAuthInterceptors();
    AuthManager.updateUI();

    // 登录表单处理
    $("#loginForm").on('submit', function(e) {
        e.preventDefault();
        const formData = {
            usernameOrEmail: $('#username').val(),
            password: $('#password').val()
        };

        $.ajax({
            url: '/api/auth/signin',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(formData),
            success: function(response) {
                if (response && response.accessToken) {
                    AuthManager.setToken(response.accessToken);
                    location.href = new URLSearchParams(location.search).get('returnUrl') || '/';
                }
            },
            error: function(xhr) {
                alert('登录失败: ' + (xhr.responseJSON?.message || '用户名或密码错误'));
            }
        });
    });

    // 退出登录处理
    $(document).on('click', '#logoutBtn', function(e) {
        e.preventDefault();
        AuthManager.logout();
    });
});