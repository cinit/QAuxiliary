//
// Created by kinit on 2023-01-03.
//

#ifndef QAUXILIARY_AUTO_CLOSE_FD_H
#define QAUXILIARY_AUTO_CLOSE_FD_H

class auto_close_fd {
 private:
  int mFd = -1;
 public:
  auto_close_fd() = default;

  explicit inline auto_close_fd(int fd) noexcept: mFd(fd) {};

  ~auto_close_fd();

  auto_close_fd(const auto_close_fd &) = delete;

  auto_close_fd &operator=(const auto_close_fd &) = delete;

  [[nodiscard]] inline bool valid() const noexcept {
      return mFd >= 0;
  }

  explicit inline operator bool() const noexcept {
      return valid();
  }

  [[nodiscard]] inline int get() const noexcept {
      return mFd;
  }

  void close() noexcept;

  inline int detach() noexcept {
      int fd = mFd;
      mFd = -1;
      return fd;
  }
};

#endif //QAUXILIARY_AUTO_CLOSE_FD_H
