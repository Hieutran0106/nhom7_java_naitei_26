// commitlint.config.js
// Cấu hình Conventional Commits: https://www.conventionalcommits.org/
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    // Type phải là một trong các loại bên dưới
    'type-enum': [
      2,
      'always',
      [
        'feat',     // Tính năng mới
        'fix',      // Sửa lỗi
        'docs',     // Thay đổi tài liệu
        'style',    // Formatting, không thay đổi logic
        'refactor', // Refactor code
        'test',     // Thêm/sửa test
        'chore',    // Cập nhật build, config, dependencies
        'perf',     // Cải thiện hiệu suất
        'ci',       // Thay đổi CI/CD
        'revert',   // Revert commit trước
        'build',    // Thay đổi build system
      ],
    ],
    // Type phải là chữ thường
    'type-case': [2, 'always', 'lower-case'],
    // Subject không được rỗng
    'subject-empty': [2, 'never'],
    // Subject không kết thúc bằng dấu chấm
    'subject-full-stop': [2, 'never', '.'],
    // Header tối đa 100 ký tự
    'header-max-length': [2, 'always', 100],
  },
};
