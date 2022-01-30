//
// Created by cinit on 2021-05-11.
//

#ifndef NATIVES_CONCURRENTPRIMITIVESET_H
#define NATIVES_CONCURRENTPRIMITIVESET_H

#include <mutex>
#include <unordered_set>
#include <atomic>

template<typename T>
class ConcurrentHashSet {
public:
    ConcurrentHashSet();

    ConcurrentHashSet(const ConcurrentHashSet &);

    ConcurrentHashSet(const std::initializer_list<T> &);

    ~ConcurrentHashSet();

    size_t size() const;

    bool isEmpty() const;

    bool contains(T &o) const;

    bool add(const T &e);

    bool add(T &&args);

    bool remove(const T &o);

//    bool containsAll(T *a, size_t size) const;

//    bool containsAll(const ConcurrentHashSet<T> &) const;

    bool addAll(const T *a, size_t size);

//    bool addAll(const ConcurrentHashSet<T> &);

    bool removeAll(const T *a, size_t size);

//    bool removeAll(const ConcurrentHashSet<T> &);

    void clear();

    ConcurrentHashSet clone() const;

private:

    explicit ConcurrentHashSet(std::unordered_set<T> *);

    std::atomic<int> *mRefCount = nullptr;
    std::mutex *mLock = nullptr;
    std::unordered_set<T> *mValues = nullptr;
};

template<typename T>
ConcurrentHashSet<T>::ConcurrentHashSet()
        :mLock(new std::mutex), mValues(new std::unordered_set<T>), mRefCount(new std::atomic<int>(1)) {
}

template<typename T>
ConcurrentHashSet<T>::ConcurrentHashSet(const std::initializer_list<T> &init_)
        :mLock(new std::mutex), mValues(new std::unordered_set<T>(init_)), mRefCount(new std::atomic<int>(1)) {
}

template<typename T>
ConcurrentHashSet<T>::ConcurrentHashSet(const ConcurrentHashSet &that)
        :mLock(that.mLock), mValues(that.mValues), mRefCount(that.mRefCount) {
    mRefCount->fetch_and(1);
}

template<typename T>
ConcurrentHashSet<T>::ConcurrentHashSet(std::unordered_set<T> *v)
        :mLock(new std::mutex), mValues(v), mRefCount(new std::atomic<int>(1)) {
}

template<typename T>
ConcurrentHashSet<T>::~ConcurrentHashSet() {
    if (mRefCount->fetch_sub(1) == 1) {
        delete mValues;
        delete mLock;
        delete mRefCount;
    }
}

template<typename T>
size_t ConcurrentHashSet<T>::size() const {
    std::lock_guard<std::mutex> _(*mLock);
    return mValues->size();
}

template<typename T>
bool ConcurrentHashSet<T>::isEmpty() const {
    return size() == 0;
}

template<typename T>
bool ConcurrentHashSet<T>::contains(T &o) const {
    std::lock_guard<std::mutex> _(*mLock);
    return mValues->find(o) != mValues->end();
}

template<typename T>
bool ConcurrentHashSet<T>::add(const T &e) {
    std::lock_guard<std::mutex> _(*mLock);
    return mValues->emplace(e).second;
}

template<typename T>
bool ConcurrentHashSet<T>::add(T &&args) {
    std::lock_guard<std::mutex> _(*mLock);
    return mValues->emplace(args).second;
}

template<typename T>
bool ConcurrentHashSet<T>::remove(const T &o) {
    std::lock_guard<std::mutex> _(*mLock);
    return mValues->erase(o);
}

template<typename T>
bool ConcurrentHashSet<T>::removeAll(const T *a, size_t size) {
    std::lock_guard<std::mutex> _(*mLock);
    bool changed = false;
    for (size_t i = 0; i < size; i++) {
        if (mValues->erase(a[i])) {
            changed = true;
        }
    }
    return changed;
}

template<typename T>
bool ConcurrentHashSet<T>::addAll(const T *a, size_t size) {
    std::lock_guard<std::mutex> _(*mLock);
    bool changed = false;
    for (size_t i = 0; i < size; i++) {
        if (mValues->emplace(a[i]).second) {
            changed = true;
        }
    }
    return changed;
}

template<typename T>
void ConcurrentHashSet<T>::clear() {
    std::lock_guard<std::mutex> _(*mLock);
    return mValues->clear();
}

template<typename T>
ConcurrentHashSet<T> ConcurrentHashSet<T>::clone() const {
    std::lock_guard<std::mutex> _(*mLock);
    auto *pval = new std::unordered_set<T>(*mValues);
    ConcurrentHashSet<T> that(pval);
    return that;
}

#endif //NATIVES_CONCURRENTPRIMITIVESET_H
