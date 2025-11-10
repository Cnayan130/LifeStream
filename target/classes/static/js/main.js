// main.js (优化版)
// 包含 LifeStream 项目的全局辅助脚本和 Bootstrap 初始化
// 移除了所有与 SPA (AuthManager, API 登录) 冲突的逻辑

/**
 * 显示一个全局 Bootstrap 警告框
 * @param {string} type - 警告类型 (e.g., 'success', 'danger', 'info')
 * @param {string} message - 要显示的消息
 */
function showAlert(type, message) {
    const alertHtml = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;

    let alertContainer = $('#alertContainer');
    if (!alertContainer.length) {
        // 自动在 main container 顶部创建容器
        const mainContainer = $('main .container').first();
        if (mainContainer.length) {
            mainContainer.prepend('<div id="alertContainer"></div>');
        } else {
            // 备用方案
            $('body').prepend('<div id="alertContainer" class="container mt-3"></div>');
        }
        alertContainer = $('#alertContainer');
    }

    alertContainer.html(alertHtml);

    // 5秒后自动消失
    setTimeout(function() {
        alertContainer.find('.alert').alert('close');
    }, 5000);
}

// 将 showAlert 暴露到全局，以便其他内联脚本可以调用
window.showAlert = showAlert;


// DOM 加载完成后执行
$(document).ready(function() {

    // --- 1. Bootstrap 初始化 ---

    // 初始化 Tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // 初始化 Popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    // --- 2. UI 辅助功能 ---

    // 返回顶部按钮
    const $scrollToTop = $('#scrollToTop');
    if ($scrollToTop.length) {
        $(window).scroll(function() {
            if ($(this).scrollTop() > 300) {
                $scrollToTop.fadeIn();
            } else {
                $scrollToTop.fadeOut();
            }
        });

        $scrollToTop.click(function() {
            $('html, body').animate({scrollTop: 0}, 800);
            return false;
        });
    }

    // 移动端导航栏切换
    $('.navbar-toggler').click(function() {
        $(this).toggleClass('open');
    });

    // 自动调整 Textarea 高度
    $('textarea.auto-resize').each(function () {
        this.setAttribute('style', 'height:' + (this.scrollHeight) + 'px;overflow-y:hidden;');
    }).on('input', function () {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });

    // 处理来自服务器的 Flash 消息
    const $flashMessage = $('.flash-message');
    if ($flashMessage.length) {
        setTimeout(function() {
            $flashMessage.fadeOut(500);
        }, 5000); // 5秒后自动隐藏
    }

    // --- 3. 表单与动作处理 ---

    // 删除确认 (使用事件委托)
    $(document).on('click', '.confirm-delete', function(e) {
        if (!confirm('确定要删除吗？此操作不可撤销。')) {
            e.preventDefault();
        }
    });

    // Bootstrap 客户端表单验证
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

    // 检查未保存的更改
    const $formsCheckChanges = $('form.check-changes');
    let formChanged = false;

    if ($formsCheckChanges.length > 0) {
        let initialFormStates = new Map();

        $formsCheckChanges.each(function() {
            const form = $(this);
            initialFormStates.set(this, form.serialize());

            form.on('change input', function() {
                formChanged = $(this).serialize() !== initialFormStates.get(this);
            });

            form.on('submit', function() {
                formChanged = false; // 提交后重置状态
                initialFormStates.set(this, $(this).serialize());
            });
        });

        window.addEventListener('beforeunload', function(e) {
            if (formChanged) {
                const message = '您有未保存的更改，确定要离开吗？';
                e.preventDefault();
                e.returnValue = message; // 兼容旧版浏览器
                return message;
            }
        });
    }

    // 通用 AJAX 表单提交 (用于评论、设置等)
    $(document).on('submit', '.ajax-form', function(e) {
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
                    // 触发一个自定义事件，以便页面的其他部分可以响
                    $(document).trigger('ajaxFormSuccess', [form, response]);
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

    // --- 4. 页面特定功能 ---

    // 上传图片预览 (使用事件委托)
    $(document).on('change', '#imageUpload', function() {
        const file = this.files[0];
        if (file && file.type.startsWith('image/')) {
            const reader = new FileReader();
            reader.onload = function(e) {
                $('#imagePreview').attr('src', e.target.result).show();
            }
            reader.readAsDataURL(file);
        }
    });

    // 图片懒加载 (使用 Intersection Observer)
    const lazyImages = document.querySelectorAll('img[data-src]');
    if (lazyImages.length > 0) {
        if ('IntersectionObserver' in window) {
            const lazyImageObserver = new IntersectionObserver(function(entries, observer) {
                entries.forEach(function(entry) {
                    if (entry.isIntersecting) {
                        const lazyImage = entry.target;
                        lazyImage.src = lazyImage.dataset.src;
                        lazyImage.removeAttribute('data-src');
                        lazyImageObserver.unobserve(lazyImage);
                    }
                });
            });

            lazyImages.forEach(function(lazyImage) {
                lazyImageObserver.observe(lazyImage);
            });
        } else {
            // 为不支持的浏览器提供回退
            lazyImages.forEach(function(lazyImage) {
                lazyImage.src = lazyImage.dataset.src;
                lazyImage.removeAttribute('data-src');
            });
        }
    }

    // 评论回复表单切换 (使用事件委托)
    $(document).on('click', '.reply-button', function() {
        const commentId = $(this).data('comment-id');
        $('#replyForm-' + commentId).slideToggle();
    });

}); // --- 结束 $(document).ready() ---