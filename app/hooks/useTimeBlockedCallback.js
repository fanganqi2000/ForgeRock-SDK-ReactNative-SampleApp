import { useRef } from 'react'

export default (callback, timeBlocked = 2000) => {
  const isBlockedRef = useRef(false)
  const unblockTimeout = useRef(false)

  return (...callbackArgs) => {
    if (!isBlockedRef.current) {
      callback(...callbackArgs)
    }
    clearTimeout(unblockTimeout.current)
    unblockTimeout.current = setTimeout(() => isBlockedRef.current = false, timeBlocked)
    isBlockedRef.current = true
  }
}