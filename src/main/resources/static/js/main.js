// main.js
$(document).ready(function() {
    // Initialize Bootstrap tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    })

    // Initialize Bootstrap popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'))
    var popoverList = popoverTriggerList.map(function(popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl)
    })

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
        const token = localStorage.getItem('jwtToken');

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
                        localStorage.removeItem('jwtToken');
                        delete axios.defaults.headers.common['Authorization'];
                    }
                })
                .catch(error => {
                    console.error('Token 验证错误', error);
                    localStorage.removeItem('jwtToken');
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
                var backdrop = document.querySelector('.modal-backdrop');
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